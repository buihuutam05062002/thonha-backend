package com.thonha.backend.repository;

import com.thonha.backend.entity.WarrantyClaim;
import com.thonha.backend.enums.WarrantyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from WarrantyClaim c where c.id = :id")
    Optional<WarrantyClaim> findByIdForUpdate(@Param("id")Long id);
    
    Optional<WarrantyClaim> findByIdAndWorkerUserId(Long id, Long userId);
    
    boolean existsByRequestIdAndStatusIn(Long requestId, Collection<WarrantyStatus> statuses);
    
    long countByWorkerUserIdAndStatusIn(Long userId, Collection<WarrantyStatus> statuses);
    
    List<WarrantyClaim> findByRequestCustomerIdOrderByCreatedAtDesc(Long customerId);

    boolean existsByQuotationIdAndStatusIn(Long quotationId, Collection<WarrantyStatus> statuses);
    
    @Query("""
                select c from WarrantyClaim c
                            where c.worker.user.id = :userId
                              and c.status in :statuses
                              and (lower(c.request.requestCode) like :keyword
                                   or lower(c.request.customer.fullName) like :keyword)
                            order by c.createdAt desc
            """)
    List<WarrantyClaim> search(@Param("userId")Long userId,
                               @Param("statuses")Collection<WarrantyStatus> statuses,
                               @Param("keyword")String keyword);

    boolean existsByQuotationId(Long quotationId);

    List<WarrantyClaim> findByRequestCustomerIdAndRequestIdOrderByCreatedAtDesc(Long customerId, Long requestId);
}
