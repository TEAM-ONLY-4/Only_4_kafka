package com.example.only4_kafka.service.email.mapper;

import com.example.only4_kafka.constant.BillItemCategoryConstant;
import com.example.only4_kafka.constant.EmailConstant;
import com.example.only4_kafka.infrastructure.MemberDataDecryptor;
import com.example.only4_kafka.repository.dto.EmailInvoiceItemRow;
import com.example.only4_kafka.repository.dto.EmailInvoiceMemberBillRow;
import com.example.only4_kafka.repository.dto.RecentBillRow;
import com.example.only4_kafka.service.email.dto.EmailInvoiceTemplateDto;
import com.example.only4_kafka.service.email.dto.EmailInvoiceReadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// DB 조회 -> Template 매핑
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailInvoiceMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(EmailConstant.DATE_FORMAT);

    private final MemberDataDecryptor memberDataDecryptor;

    public EmailInvoiceTemplateDto toDto(EmailInvoiceReadResult emailInvoiceReadResult) {
        EmailInvoiceMemberBillRow memberBill = emailInvoiceReadResult.memberBill();
        List<EmailInvoiceItemRow> itemList = emailInvoiceReadResult.itemList();
        List<RecentBillRow> recentBillRowList = emailInvoiceReadResult.recentBillList();

        // 상품/할인 분류
        List<EmailInvoiceTemplateDto.Product> productList = new ArrayList<>();
        List<EmailInvoiceTemplateDto.Discount> discountList = new ArrayList<>();
        BigDecimal monthlyFee = BigDecimal.ZERO;
        BigDecimal additionalFee = BigDecimal.ZERO;

        for (EmailInvoiceItemRow item : itemList) {
            if (BillItemCategoryConstant.DISCOUNT.equals(item.category())) {
                discountList.add(new EmailInvoiceTemplateDto.Discount(item.name(), item.amount()));
            } else {
                productList.add(new EmailInvoiceTemplateDto.Product(
                        item.name(),
                        item.amount(),
                        item.category(),
                        item.subcategory(),
                        item.detailSnapshot()
                ));

                // 카테고리별 금액 합산
                if (BillItemCategoryConstant.SUBSCRIPTION.equals(item.category())) {
                    monthlyFee = monthlyFee.add(item.amount());
                } else {
                    additionalFee = additionalFee.add(item.amount());
                }
            }
        }

        // 최근 4개월 매핑
        List<EmailInvoiceTemplateDto.RecentMonth> recentMonthList = mapRecentMonths(recentBillRowList);

        // 민감정보 복호화
        String decryptedPhoneNumber = memberDataDecryptor.decryptPhoneNumber(memberBill.memberPhoneNumber());
        log.info("3-1) 복호화 휴대전화. decryptedPhoneNumber={}", decryptedPhoneNumber);
        return new EmailInvoiceTemplateDto(
                // 기본 정보
                memberBill.billingYearMonth().getYear(),
                memberBill.billingYearMonth().getMonthValue(),
                memberBill.memberName(),
                decryptedPhoneNumber,
                memberBill.memberGrade(),
                memberBill.memberAddress(),

                // 청구서 정보
                generateInvoiceNumber(memberBill.billId(), memberBill.billingYearMonth()),
                generateUsagePeriod(memberBill.billingYearMonth()),
                formatDate(memberBill.createdDate()),
                formatDate(memberBill.dueDate()),

                // 금액 정보
                memberBill.totalBilledAmount(),
                memberBill.totalAmount(),
                memberBill.totalDiscountAmount(),
                memberBill.unpaidAmount(),

                // 당월요금 상세
                monthlyFee,
                BigDecimal.ZERO,  // roundingDiscount - 별도 계산 필요시 추가
                memberBill.vat(),
                additionalFee,

                // 최근 4개월
                recentMonthList,

                // 상품/할인
                productList,
                discountList,
                memberBill.totalDiscountAmount(),

                // 결제정보
                memberBill.paymentOwnerNameSnapshot(),
                memberBill.paymentNameSnapshot(),
                memberBill.paymentNumberSnapshot()
        );
    }

    // 최근 4개월 변환
    private List<EmailInvoiceTemplateDto.RecentMonth> mapRecentMonths(List<RecentBillRow> recentBillRowList) {
        List<EmailInvoiceTemplateDto.RecentMonth> recentMonthList = new ArrayList<>();

        for (RecentBillRow recentBillRow : recentBillRowList) {
            recentMonthList.add(new EmailInvoiceTemplateDto.RecentMonth(
                    formatYearMonth(recentBillRow.billingYearMonth()),
                    recentBillRow.monthlyFee(),
                    recentBillRow.additionalFee(),
                    BigDecimal.ZERO,  // roundingDiscount
                    recentBillRow.vat(),
                    recentBillRow.totalDiscountAmount(),
                    recentBillRow.totalBilledAmount()
            ));
        }

        return recentMonthList;
    }

    // 청구서 번호 생성
    private String generateInvoiceNumber(long billId, LocalDate billingYearMonth) {
        // 형식: YYYYMM + billId (7자리)
        return String.format("%d%02d%07d",
                billingYearMonth.getYear(),
                billingYearMonth.getMonthValue(),
                billId);
    }

    // 이용기간 문자열 포맷 생성
    private String generateUsagePeriod(LocalDate billingYearMonth) {
        // 이전 달의 1일 ~ 말일
        LocalDate usageMonth = billingYearMonth.minusMonths(1);
        LocalDate startDate = usageMonth.withDayOfMonth(1);
        LocalDate endDate = usageMonth.withDayOfMonth(usageMonth.lengthOfMonth());

        return String.format("%d.%d.%d ~ %d.%d",
                startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth(),
                endDate.getMonthValue(), endDate.getDayOfMonth());
    }

    // 날짜 → 문자열 포맷
    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DATE_FORMATTER);
    }

    // 연월 → 문자열 포맷
    private String formatYearMonth(LocalDate date) {
        return String.format("%d.%d", date.getYear(), date.getMonthValue());
    }
}
