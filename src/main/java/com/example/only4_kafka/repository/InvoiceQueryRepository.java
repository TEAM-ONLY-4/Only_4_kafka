package com.example.only4_kafka.repository;

import com.example.only4_kafka.domain.bill_notification.BillChannel;
import com.example.only4_kafka.domain.bill_notification.SendStatus;
import com.example.only4_kafka.domain.bill_send.SmsBillDto;
import com.example.only4_kafka.repository.dto.BillNotificationRow;
import com.example.only4_kafka.repository.dto.EmailInvoiceItemRow;
import com.example.only4_kafka.repository.dto.EmailInvoiceMemberBillRow;
import com.example.only4_kafka.repository.dto.RecentBillRow;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class InvoiceQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<EmailInvoiceMemberBillRow> findMemberBill(Long memberId, Long billId) {
        return jdbcTemplate.query(
                """
                SELECT
                    m.id AS member_id,
                    m.name AS member_name,
                    m.phone_number AS member_phone_number,
                    m.member_grade AS member_grade,
                    m.address AS member_address,
                    m.email AS member_email,
                    m.payment_method AS member_payment_method,
                    b.id AS bill_id,
                    b.billing_year_month AS billing_year_month,
                    b.total_amount AS total_amount,
                    b.vat AS vat,
                    b.unpaid_amount AS unpaid_amount,
                    b.total_discount_amount AS total_discount_amount,
                    b.total_billed_amount AS total_billed_amount,
                    b.due_date AS due_date,
                    b.created_date AS created_date,
                    b.payment_owner_name_snapshot AS payment_owner_name_snapshot,
                    b.payment_name_snapshot AS payment_name_snapshot,
                    b.payment_number_snapshot AS payment_number_snapshot
                FROM member m
                JOIN bill b ON b.member_id = m.id
                WHERE m.id = ? AND b.id = ?
                """,
                this::mapMemberBillRow,
                memberId,
                billId
        ).stream().findFirst();
    }

    public List<EmailInvoiceItemRow> findBillItems(Long billId) {
        return jdbcTemplate.query(
                """
                SELECT
                    item_category,
                    item_subcategory,
                    item_name,
                    amount,
                    detail_snapshot
                FROM bill_item
                WHERE bill_id = ?
                ORDER BY id
                """,
                this::mapItemRow,
                billId
        );
    }

    public List<RecentBillRow> findRecentBills(Long memberId, LocalDate currentBillingYearMonth, int months) {
        return jdbcTemplate.query(
                """
                SELECT
                    b.billing_year_month,
                    b.total_amount,
                    b.vat,
                    b.total_discount_amount,
                    b.total_billed_amount,
                    COALESCE(SUM(CASE WHEN bi.item_category = 'SUBSCRIPTION' THEN bi.amount ELSE 0 END), 0) AS monthly_fee,
                    COALESCE(SUM(CASE WHEN bi.item_category IN ('OVER_USAGE', 'ONE_TIME_PURCHASE') THEN bi.amount ELSE 0 END), 0) AS additional_fee
                FROM bill b
                LEFT JOIN bill_item bi ON bi.bill_id = b.id
                WHERE b.member_id = ?
                  AND b.billing_year_month <= ?
                  AND b.status = 'ACTIVE'
                GROUP BY b.id, b.billing_year_month, b.total_amount, b.vat, b.total_discount_amount, b.total_billed_amount
                ORDER BY b.billing_year_month DESC
                LIMIT ?
                """,
                this::mapRecentBillRow,
                memberId,
                currentBillingYearMonth,
                months
        );
    }

    // billNotification 가져오기
    public Optional<BillNotificationRow> findBillNotification(Long billId) {
        return jdbcTemplate.query(
                """
                SELECT
                    member_id,
                    bill_id,
                    channel,
                    send_status,
                    process_start_time
                FROM bill_notification
                WHERE bill_id = ?
                """,
                this::mapBillNotificationRow,
                billId
        ).stream().findFirst();
    }

    private RecentBillRow mapRecentBillRow(ResultSet rs, int rowNum) throws SQLException {
        return new RecentBillRow(
                rs.getObject("billing_year_month", LocalDate.class),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("vat"),
                rs.getBigDecimal("total_discount_amount"),
                rs.getBigDecimal("total_billed_amount"),
                rs.getBigDecimal("monthly_fee"),
                rs.getBigDecimal("additional_fee")
        );
    }

    private EmailInvoiceMemberBillRow mapMemberBillRow(ResultSet rs, int rowNum) throws SQLException {
        return new EmailInvoiceMemberBillRow(
                rs.getLong("member_id"),
                rs.getString("member_name"),
                rs.getString("member_phone_number"),
                rs.getString("member_grade"),
                rs.getString("member_address"),
                rs.getString("member_email"),
                rs.getString("member_payment_method"),
                rs.getLong("bill_id"),
                rs.getObject("billing_year_month", LocalDate.class),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("vat"),
                rs.getBigDecimal("unpaid_amount"),
                rs.getBigDecimal("total_discount_amount"),
                rs.getBigDecimal("total_billed_amount"),
                rs.getObject("due_date", LocalDate.class),
                rs.getObject("created_date", LocalDate.class),
                rs.getString("payment_owner_name_snapshot"),
                rs.getString("payment_name_snapshot"),
                rs.getString("payment_number_snapshot")
        );
    }

    private EmailInvoiceItemRow mapItemRow(ResultSet rs, int rowNum) throws SQLException {
        return new EmailInvoiceItemRow(
                rs.getString("item_category"),
                rs.getString("item_subcategory"),
                rs.getString("item_name"),
                rs.getBigDecimal("amount"),
                rs.getString("detail_snapshot")
        );
    }

    // BillNotification -> BillNotificationRow DTO 매핑
    private BillNotificationRow mapBillNotificationRow(ResultSet rs, int rowNum) throws SQLException {
        return new BillNotificationRow(
                rs.getLong("member_id"),
                rs.getLong("bill_id"),
                // DB의 문자열(VARCHAR/ENUM)을 자바 Enum으로 변환
                BillChannel.valueOf(rs.getString("channel")),
                SendStatus.valueOf(rs.getString("send_status")),
                rs.getObject("process_start_time", LocalDateTime.class)
        );
    }

    public Optional<SmsBillDto> findSmsBillDto(Long billId, LocalDate now) {
        return jdbcTemplate.query(
                """
                SELECT
                    m.name,
                    m.phone_number,
                    m.do_not_disturb_start_time,
                    m.do_not_disturb_end_time,
                    b.payment_owner_name_snapshot,
                    b.payment_name_snapshot,
                    b.payment_number_snapshot,
                    b.due_date,
                    b.id AS bill_id,
                    b.billing_year_month,
                    b.total_amount,
                    b.total_discount_amount,
                    b.unpaid_amount,
                    b.total_billed_amount,
                    b.vat,
                    (
                        SELECT COALESCE(SUM(bi.amount), 0)
                        FROM bill_item bi
                        WHERE bi.bill_id = b.id
                          AND bi.item_category = 'SUBSCRIPTION'
                    ) AS total_monthly_amount,
                    (
                        SELECT COALESCE(SUM(bi.amount), 0)
                        FROM bill_item bi
                        WHERE bi.bill_id = b.id
                          AND bi.item_category = 'OVER_USAGE'
                    ) AS total_overage_amount,
                    (
                        SELECT COALESCE(SUM(bi.amount), 0)
                        FROM bill_item bi
                        WHERE bi.bill_id = b.id
                          AND bi.item_category = 'ONE_TIME_PURCHASE'
                    ) AS total_micro_amount
                FROM bill b
                JOIN member m ON b.member_id = m.id
                WHERE b.id = ?
                """,
                (rs, rowNum) -> mapSmsBillDto(rs, now),
                billId
        ).stream().findFirst();
    }

    private SmsBillDto mapSmsBillDto(ResultSet rs, LocalDate now) throws SQLException {
        return new SmsBillDto(
                rs.getString("name"),
                rs.getString("phone_number"),
                rs.getObject("do_not_disturb_start_time", LocalTime.class),
                rs.getObject("do_not_disturb_end_time", LocalTime.class),
                rs.getString("payment_owner_name_snapshot"),
                rs.getString("payment_name_snapshot"),
                rs.getString("payment_number_snapshot"),
                rs.getObject("due_date", LocalDate.class),
                rs.getLong("bill_id"),
                rs.getObject("billing_year_month", LocalDate.class),
                rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("total_discount_amount"),
                rs.getBigDecimal("unpaid_amount"),
                rs.getBigDecimal("total_billed_amount"),
                rs.getBigDecimal("total_monthly_amount"),
                rs.getBigDecimal("total_overage_amount"),
                rs.getBigDecimal("total_micro_amount"),
                rs.getBigDecimal("vat"),
                now
        );
    }
}
