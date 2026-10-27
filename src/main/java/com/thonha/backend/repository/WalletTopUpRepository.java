package com.thonha.backend.repository;

import com.thonha.backend.entity.WalletTopUp;
import com.thonha.backend.enums.TransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletTopUpRepository extends JpaRepository<WalletTopUp, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from WalletTopUp t join fetch t.worker where t.id = :id")
    Optional<WalletTopUp> findByIdForUpdate(@Param("id") Long id);

    Optional<WalletTopUp> findByIdAndWorkerId(Long id, Long workerId);

    Optional<WalletTopUp> findFirstByWorkerIdAndAmountAndStatusOrderByIdDesc(
            Long workerId, BigDecimal amount, TransactionStatus status);
}
