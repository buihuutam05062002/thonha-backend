package com.thonha.backend.repository;

import com.thonha.backend.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByWorkerId(Long workerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.worker.id = :workerId")
    Optional<Wallet> findByWorkerIdForUpdate(@Param("workerId") Long workerId);

    @Query(value = "select w from Wallet w join fetch w.worker wp join fetch wp.user u " +
            "where (:keyword is null or lower(u.fullName) like lower(concat('%', :keyword, '%')) " +
            "      or lower(u.email) like lower(concat('%', :keyword, '%')) " +
            "      or u.phoneNumber like concat('%', :keyword, '%')) " +
            "and (:negativeOnly = false or w.balance < 0)",
            countQuery = "select count(w) from Wallet w join w.worker wp join wp.user u " +
            "where (:keyword is null or lower(u.fullName) like lower(concat('%', :keyword, '%')) " +
            "      or lower(u.email) like lower(concat('%', :keyword, '%')) " +
            "      or u.phoneNumber like concat('%', :keyword, '%')) " +
            "and (:negativeOnly = false or w.balance < 0)")
    Page<Wallet> search(@Param("keyword") String keyword,
                        @Param("negativeOnly") boolean negativeOnly,
                        Pageable pageable);

    @Query("select wp.id from WorkerProfile wp " +
            "where wp.approvalStatus = com.thonha.backend.enums.ApprovalStatus.APPROVED " +
            "and not exists (select 1 from Wallet w where w.worker = wp)")
    List<Long> findApprovedWorkerIdsWithoutWallet();
}
