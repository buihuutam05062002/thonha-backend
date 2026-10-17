package com.thonha.backend.repository;

import com.thonha.backend.entity.Quotation;
import com.thonha.backend.enums.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    @Query("select q from Quotation q join fetch q.worker w join fetch w.user " +
            "where q.request.id = :requestId order by q.id desc")
    List<Quotation> findByRequestWithWorker(@Param("requestId") Long requestId);

    @Query("select q from Quotation q join fetch q.request r join fetch r.customer join fetch q.worker w join fetch w.user " +
            "where q.id = :id")
    Optional<Quotation> findByIdWithDetails(@Param("id") Long id);

    @Query("select q.request.id from Quotation q where q.id = :id")
    Optional<Long> findRequestId(@Param("id") Long id);

    /** Tổng tiền các báo giá ở trạng thái `status` của yêu cầu; null nếu chưa có báo giá nào. */
    @Query("select sum(q.totalAmount) from Quotation q where q.request.id = :requestId and q.status = :status")
    BigDecimal sumAmount(@Param("requestId") Long requestId, @Param("status") QuotationStatus status);

    List<Quotation> findByRequestIdAndStatus(Long requestId, QuotationStatus status);
    /** Chuyển các báo giá đang ở trạng thái `from` của yêu cầu sang `to` (trừ báo giá `exceptId`). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Quotation q set q.status = :to where q.request.id = :requestId and q.status = :from and q.id <> :exceptId")
    int transition(@Param("requestId") Long requestId, @Param("from") QuotationStatus from,
                   @Param("to") QuotationStatus to, @Param("exceptId") Long exceptId);

    /** Chuyển các báo giá đang ở trạng thái `from` của yêu cầu sang `to`. */
    @Modifying(flushAutomatically = true)
    @Query("update Quotation q set q.status = :to where q.request.id = :requestId and q.status = :from")
    int transition(@Param("requestId") Long requestId, @Param("from") QuotationStatus from,
                   @Param("to") QuotationStatus to);
}
