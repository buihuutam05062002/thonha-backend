package com.thonha.backend.repository;

import com.thonha.backend.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<WalletTransaction> findByWalletIdOrderByIdDesc(Long walletId, Pageable pageable);
}
