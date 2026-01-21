package com.example.only4_kafka.domain.bill_notification;

import com.example.only4_kafka.domain.bill.Bill;
import com.example.only4_kafka.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "bill_notification")
public class BillNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    // 알림 대상자 ID (성능을 위해 반정규화 or 직접 참조)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bill_notification_to_bill"))
    private Bill bill;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "channel", nullable = false, columnDefinition = "bill_channel_enum")
    private BillChannel channel;

//    @Enumerated(EnumType.STRING)
//    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
//    @Column(name = "send_status", nullable = false, columnDefinition = "bill_notification_status_enum")
//    private SendStatus sendStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "publish_status", nullable = false, columnDefinition = "publish_status_enum")
    private PublishStatus publishStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "send_status", nullable = false, columnDefinition = "send_status_enum")
    private SendStatus sendStatus;

    @Column(name = "process_start_time", nullable = false) // nullable이 맞는지?
    private LocalDateTime processStartTime;

    public void changeSendStatus(SendStatus sendStatus) {
        this.sendStatus = sendStatus;
    }

    public void changeProcessStartTime(LocalDateTime processStartTime) {
        this.processStartTime = processStartTime;
    }

    public void changeBillChannel(BillChannel channel) {
        this.channel = channel;
    }
}