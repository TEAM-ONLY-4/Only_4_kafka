# only4_kafka - 대용량 알림 전송 시스템 (메시지 플랫폼)

Only4_Backed의 발송 배치 결과가 Kafka Broker에 전달된 후, Kafka Consumer가 메시지를 소비하며

**Email Producer** ➔ **Email Consumer** ➔ **SMS Producer** ➔ **SMS Consumer** ➔ **DLT** Topic으로 메시지가 전달되는 시스템

only4_kafka ➔ **메시지 플랫폼**

Only4_Backend ➔ API 서버 + 정산 / 발송 배치

---

## 요구사항
### 1. 청구서 발송 대상 확인 및 실제 청구서 생성
- 배치에서 발송 대상자로 선정된 청구서 정보를 전달받는다.
- 전달받은 청구서 정보를 기반으로 실제 발송용 청구서 메시지를 생성한다.
    - 암호화된 데이터 디코딩 및 복호화
    - 개인정보 마스킹 처리
    - HTML 이메일 / SMS 템플릿 적용

### 2. 이메일 기반 1차 발송
- 생성된 청구서는 이메일을 통해 1차 발송한다.
- 이메일 발송은 실제 메일 서버 대신 1초 지연(Delay) 방식으로 시뮬레이션한다.
- 이메일 발송은 약 1% 확률로 실패하도록 한다.

### 3. 이메일 발송 실패 시 SMS 대체 발송
- 이메일 발송이 최종 실패한 경우, 동일한 청구서를 SMS로 대체 발송한다.
- SMS 발송 역시 중복 발송을 방지해야 하며, 발송 상태를 명확히 관리한다.

### 4. 발송 실패 및 재전송 관리
- 이메일 및 SMS 발송이 모두 실패한 청구서는 최종 실패 상태로 관리한다.
- 최종 실패된 청구서는 이후 재전송 대상으로 관리된다.

### 5. 메시지 신뢰성 보장
- 모든 청구서 메시지는 중복 발송 되어서는 안된다.
- 장애, 재시도, 비정상 종료 상황에서도 발송 상태의 일관성이 유지되어야 한다.
- 외부 API와 연동되므로 정확히 한 번 전달이 어렵다면 유실에 대한 추적이 가능하도록 로그 및 DLT를 활용한다.


---

## 📨 메시지 플랫폼 구조 및 흐름

### [System] 메시지 플랫폼

#### [Producer] Event Publisher
Topic 발행
- Email Producer
- SMS Producer
- DLT

#### [Consumer] Event Listener
메시지 선점 → 청구서 생성 → 청구서 발송
- Email Consumer
- SMS Consumer

---

## 🎬 Producer
**1. Topic 선정**
* KafkaProperties에서 발송 플랫폼에 따른 Topic 선정

    
**2. 메시지 발송**
* <Topic 이름, Key (청구서 id), 이벤트 Dto> 형태의 메시지를 Kafka Broker에 비동기 전송 


## 🛍️ Consumer
**1. 메시지 선점 시도**
* 발송 상태 & 선점 시각 비교해 메시지 소비 / 취소 선택 후 선점

**2. 청구서 데이터 조회**
* 이벤트 id에 포함된 회원 id, 청구서 id로 데이터 조회 후 청구서 DTO 생성

**3. 데이터 복호화**
* 이메일, 전화번호 정보 디코딩 **(Base64)** & 복호화 **(AES-256)**

**4. 템플릿 렌더링**
* HTML / TEXT 형태의 청구서 렌더링

**5. 청구서 전송 상태 변경**
* 중복 발송 방지를 위해 발송 전 발소 상태를 **SENT**로 변경

**6. 알림 발송**
* 발송 성공 시 종료
* 발송 실패 시 SMS / DLT 토픽 발행

---

## 💻 Consumer 고도화 과정
### 1. 일반 컨슈머
(이미지 추가 필요)
Kafka의 기본 KafkaConsumer를 사용하여 단일 스레드가 poll() → process() → commit을 순차적으로 수행.

로직: 메시지를 하나 가져와서 이메일을 발송하고(1초 대기), 성공하면 오프셋을 커밋함.

문제점 🚨: HOL(Head-of-Line) Blocking
- 이메일 발송에 1초가 걸리면, 처리량(Throughput)은 초당 1건으로 고정됨.
- 100만 건 발송 시 약 100만초 (약 11일)이 소요되는 심각한 성능 저하 발생.

### 2. 일반 컨슈머 + 이메일 발송 비동기 병렬 처리 (@Async)
(이미지 추가 필요)
구조: 메시지 소비(Main Thread)와 이메일 발송(Worker Thread)을 분리.

로직: 메인 스레드는 poll()만 수행하고, CompletableFuture를 이용해 별도 스레드풀에 발송 작업을 위임 후 즉시 오프셋 커밋.

문제점 🚨: 데이터 유실 및 정합성 붕괴 (Data Consistency)
- Offset Inconsistency: 발송 작업이 끝나기도 전에 메인 스레드가 오프셋을 커밋해버림
- 만약 발송 중 에러가 발생하거나 서버가 다운되면, Kafka는 해당 메시지를 "처리 완료"로 간주하므로 영구적인 데이터 유실 발생.
- Backpressure(배압) 조절이 어려워 OutOfMemoryError 위험 존재.

### 3. 병렬 컨슈머
(이미지 추가 필요)
구조: Confluent의 Parallel Consumer 라이브러리를 도입.

로직: 파티션 수에 종속되지 않고, 컨슈머 내부에서 메시지를 키(Key) 단위로 분배하여 병렬 스레드에서 처리. Kafka 파티션을 늘리지 않고도 동시성 확장 가능.

문제점 🚨: 컨텍스트 스위칭 오버헤드 (Context Switching)
- I/O Blocking(1초)을 버티기 위해 스레드 풀 사이즈를 수백 ~ 수천 개로 늘려야 함.
- Java의 Platform Thread(OS Thread)는 생성 비용이 비싸고(개당 약 1MB), 많은 스레드가 대기 상태에 있을 때 CPU 컨텍스트 스위칭 비용이 급증하여 시스템 부하 가중.

### 4. 병렬 컨슈머 + 이메일 발송 비동기 병렬 처리
(이미지 추가 필요)
구조: Parallel Consumer가 지원하는 비동기 모드 활용.

로직: 발송 로직 자체를 비동기로 작성하고, Parallel Consumer가 Future의 완료 시점을 감지하여 오프셋을 커밋하도록 변경. (데이터 유실 문제 해결)

문제점 🚨: 스레드 풀 고갈 (Thread Pool Exhaustion)
- 로직은 비동기지만, 결국 내부적으로 이메일 발송(1초 지연)을 수행하는 동안 실제 OS 스레드는 블로킹(Blocking) 상태로 묶여 있음.
- 동시 처리량을 높이려면 여전히 물리적인 스레드 개수를 무한정 늘려야 하는 하드웨어적 한계에 봉착.

### 5. 병렬 컨슈머 + 이메일 발송 비동기 병렬 처리 + 가상 스레드 사용
(이미지 추가 필요)
구조: JDK 21의 가상 스레드(Virtual Threads) 적용.

로직: Parallel Consumer의 작업 수행 스레드를 Platform Thread가 아닌 Virtual Thread로 교체.

**결과 : Non-blocking I/O 효과 극대화**
- 이메일 발송(1초 대기) 시, 가상 스레드는 즉시 캐리어 스레드(OS 스레드)에서 언마운트(Unmount)됨.
- 적은 수의 OS 스레드만으로도 수천, 수만 개의 동시 발송 요청을 처리 가능.
- 메모리 사용량 최소화 및 처리량(Throughput) 획기적 개선 달성.

---

## 메시지 중복 발송 처리 및 장애 대응
### 멱등성 프로듀서
``` java
enable.idempotence = true
```
네트워크 레벨의 패킷 중복 자동 제거

### DB 원자적 선점
``` java
UPDATE ... WHERE status = 'PENDING'
```
단 하나의 스레드만 비즈니스 로직에 진입

### Timeout
``` java
WHERE status = 'SENDING' AND 현재 시각 > 선점시각 + 작업 시간
```
좀비 프로세스이므로 선점 후 작업 수행


``` java
WHERE status = 'SENDING' AND 현재 시각 > 선점시각 + 작업 시간
```
다른 컨슈머가 작업 중인 메시지이므로 작업 수행


---

## 📂 상세 프로젝트 구조

```text
src/main/java/com/example/only4_kafka
├── 📂 config                   # 시스템 전반의 설정
│   ├── 📂 kafka                # Kafka 토픽, 컨슈머, 리트라이, Parallel Consumer 설정
│   ├── 📂 properties           # @ConfigurationProperties를 이용한 설정값 매핑
│   └── ThreadPoolConfig.java   # 비동기 처리를 위한 스레드풀 설정
│
├── 📂 controller               # API 엔드포인트
│   └── TestController.java     # Kafka 이벤트 발행 테스트용 API
│
├── 📂 domain                   # 핵심 비즈니스 도메인 모델 및 JPA Entity
│   ├── 📂 bill                 # 청구서 (Bill) 관련 엔티티 및 리포지토리
│   ├── 📂 bill_item            # 청구 항목 (BillItem) 상세 정보
│   ├── 📂 bill_notification    # 알림 발송 상태 및 채널 관리
│   ├── 📂 member               # 회원 정보 및 등급 관리
│   └── 📂 product              # 요금제 및 부가서비스 정보
│
├── 📂 event                    # Kafka 메시지 발행을 위한 이벤트 DTO
│   ├── EmailSendRequestEvent.java
│   └── SmsSendRequestEvent.java
│
├── 📂 global                   # 공통 관심사 (Cross-cutting Concerns)
│   ├── 📂 common               # 공통 응답 DTO (SuccessResponse, PageResponse)
│   ├── 📂 config               # JPA, Thymeleaf 등 프레임워크 설정
│   └── 📂 exception            # 전역 예외 처리 (GlobalExceptionHandler, ErrorCode)
│
├── 📂 infrastructure           # 외부 시스템 연동 및 기술 구현체
│   ├── 📂 email                # 실제 이메일 발송 클라이언트 (Mock)
│   ├── 📂 sms                  # 실제 SMS 발송 클라이언트 (Mock)
│   ├── MemberDataDecryptor.java # 회원 데이터 복호화
│   └── MemberDataMasker.java    # 개인정보 마스킹 처리
│
├── 📂 listener                 # Kafka 메시지 구독 (Consumer)
│   ├── EmailParallelConsumer.java # Parallel Consumer 기반 고성능 이메일 처리
│   └── SmsRequestListener.java    # 표준 리스너 기반 SMS 처리
│
├── 📂 repository               # 복잡한 쿼리 처리를 위한 리포지토리 계층
│   └── InvoiceQueryRepository.java # 명세서 조회를 위한 최적화된 쿼리 (Join 등)
│
└── 📂 service                  # 비즈니스 로직 계층
    ├── 📂 email                # 이메일 발송 관련 서비스 (Reader, Mapper, Renderer)
    │   └── fallback            # 이메일 최종 실패 시 SMS 전환 로직
    └── 📂 sms                  # SMS 발송 관련 서비스 (Reader, Mapper, Renderer)
```




