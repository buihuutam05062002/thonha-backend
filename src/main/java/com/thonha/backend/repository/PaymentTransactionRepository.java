package com.thonha.backend.repository;

import com.thonha.backend.entity.PaymentTransaction;
import com.thonha.backend.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findFirstByRequestIdAndStatus(Long requestId, TransactionStatus status);

    @EntityGraph(attributePaths = {"request", "request.worker", "request.worker.user"})
    @Query("select t from PaymentTransaction t " +
            "where t.status = :status and t.createdAt >= :from and t.createdAt < :to")
    Page<PaymentTransaction> search(@Param("status") TransactionStatus status,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to,
                                    Pageable pageable);

    @Query("select count(t) as orders, sum(t.amount) as gross, sum(t.commissionAmount) as commission, " +
            "sum(t.workerAmount) as workerTotal " +
            "from PaymentTransaction t " +
            "where t.status = :status and t.createdAt >= :from and t.createdAt < :to")
    SummaryRow summarize(@Param("status") TransactionStatus status,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to);

    /** Như summarize nhưng chỉ tính các giao dịch của đơn do một thợ phụ trách. */
    @Query("select count(t) as orders, sum(t.amount) as gross, sum(t.commissionAmount) as commission, " +
            "sum(t.workerAmount) as workerTotal " +
            "from PaymentTransaction t " +
            "where t.status = :status and t.request.worker.id = :workerId " +
            "and t.createdAt >= :from and t.createdAt < :to")
    SummaryRow summarizeByWorker(@Param("workerId") Long workerId,
                                 @Param("status") TransactionStatus status,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    @Query("select t.createdAt as createdAt, t.amount as amount, t.commissionAmount as commission, " +
            "t.workerAmount as workerAmount " +
            "from PaymentTransaction t " +
            "where t.status = :status and t.request.worker.id = :workerId " +
            "and t.createdAt >= :from and t.createdAt < :to order by t.createdAt")
    List<EntryRow> findEntriesByWorker(@Param("workerId") Long workerId,
                                       @Param("status") TransactionStatus status,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    interface EntryRow {
        LocalDateTime getCreatedAt();

        BigDecimal getAmount();

        BigDecimal getCommission();

        BigDecimal getWorkerAmount();
    }

    interface SummaryRow {
        Long getOrders();

        BigDecimal getGross();

        BigDecimal getCommission();

        BigDecimal getWorkerTotal();
    }

    @Query("select t.createdAt as createdAt, t.amount as amount, t.commissionAmount as commission, " +
            "t.workerAmount as workerAmount " +
            "from PaymentTransaction t " +
            "where t.status = :status and t.createdAt >= :from and t.createdAt < :to order by t.createdAt")
    List<EntryRow> findEntries(@Param("status") TransactionStatus status,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    @Query("select w.id, u.fullName, count(t), sum(t.amount), sum(t.commissionAmount) " +
            "from PaymentTransaction t join t.request r join r.worker w join w.user u " +
            "where t.status = :status and t.createdAt >= :from and t.createdAt < :to " +
            "group by w.id, u.fullName order by sum(t.amount) desc")
    List<Object[]> topWorkers(@Param("status") TransactionStatus status,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              Pageable pageable);
}
