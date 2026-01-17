package com.example.only4_kafka.service.email;

import com.example.only4_kafka.repository.dto.EmailInvoiceItemRow;
import com.example.only4_kafka.repository.dto.EmailInvoiceMemberBillRow;
import com.example.only4_kafka.repository.dto.RecentBillRow;
import com.example.only4_kafka.service.email.dto.EmailInvoiceTemplateDto;
import com.example.only4_kafka.service.email.dto.EmailInvoiceReadResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// DB 조회 -> Template 매핑
@Component
public class EmailInvoiceMapper {

    private static final String CATEGORY_DISCOUNT = "DISCOUNT";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.M.d");

    public EmailInvoiceTemplateDto toDto(EmailInvoiceReadResult emailInvoiceReadResult) {
        EmailInvoiceMemberBillRow memberBill = emailInvoiceReadResult.memberBill();
        List<EmailInvoiceItemRow> items = emailInvoiceReadResult.items();
        List<RecentBillRow> recentBillRows = emailInvoiceReadResult.recentBills();

        // 상품/할인 분류
        List<EmailInvoiceTemplateDto.Product> products = new ArrayList<>();
        List<EmailInvoiceTemplateDto.Discount> discounts = new ArrayList<>();
        BigDecimal monthlyFee = BigDecimal.ZERO;
        BigDecimal additionalFee = BigDecimal.ZERO;

        for (EmailInvoiceItemRow item : items) {
            if (CATEGORY_DISCOUNT.equals(item.category())) {
                discounts.add(new EmailInvoiceTemplateDto.Discount(item.name(), item.amount()));
            } else {
                products.add(new EmailInvoiceTemplateDto.Product(
                        item.name(),
                        item.amount(),
                        item.category(),
                        item.subcategory(),
                        item.detailSnapshot()
                ));

                // 카테고리별 금액 합산
                if ("SUBSCRIPTION".equals(item.category())) {
                    monthlyFee = monthlyFee.add(item.amount());
                } else {
                    additionalFee = additionalFee.add(item.amount());
                }
            }
        }

        // 최근 4개월 매핑
        List<EmailInvoiceTemplateDto.RecentMonth> recentMonthList = mapRecentMonths(recentBillRows);

        return new EmailInvoiceTemplateDto(
                // 기본 정보
                memberBill.billingYearMonth().getYear(),
                memberBill.billingYearMonth().getMonthValue(),
                memberBill.memberName(),
                memberBill.memberPhoneNumber(),
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
                products,
                discounts,
                memberBill.totalDiscountAmount(),

                // 결제정보
                memberBill.paymentOwnerNameSnapshot(),
                memberBill.paymentNameSnapshot(),
                memberBill.paymentNumberSnapshot()
        );
    }

    // 최근 4개월 변환
    private List<EmailInvoiceTemplateDto.RecentMonth> mapRecentMonths(List<RecentBillRow> recentBillRows) {
        List<EmailInvoiceTemplateDto.RecentMonth> recentMonths = new ArrayList<>();

        for (RecentBillRow row : recentBillRows) {
            recentMonths.add(new EmailInvoiceTemplateDto.RecentMonth(
                    formatYearMonth(row.billingYearMonth()),
                    row.monthlyFee(),
                    row.additionalFee(),
                    BigDecimal.ZERO,  // roundingDiscount
                    row.vat(),
                    row.totalDiscountAmount(),
                    row.totalBilledAmount()
            ));
        }

        return recentMonths;
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
