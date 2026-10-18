package com.thonha.backend.repository;

import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.enums.RepairStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepairRequestRepository extends JpaRepository<RepairRequest, Long>, JpaSpecificationExecutor<RepairRequest> {
    @Override
    @EntityGraph(attributePaths = {"category", "customer", "address", "worker", "worker.user"})
    Page<RepairRequest> findAll(Specification<RepairRequest> spec, Pageable pageable);

    List<RepairRequest> findByCustomerIdOrderByCreatedAtDesc(Long userId);

    Optional<RepairRequest> findByIdAndCustomerId(Long id, Long userId);

    List<RepairRequest> findByWorkerIdOrderByCreatedAtDesc(Long workerId);

    @Query("select r from RepairRequest r join fetch r.category join fetch r.customer where r.id = :id")
    Optional<RepairRequest> findByIdWithDetails(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RepairRequest r where r.id = :id")
    Optional<RepairRequest> findByIdForUpdate(@Param("id") Long id);


    @Query("select r from RepairRequest r where r.customer.id = :userId and r.status in :statuses order by r.createdAt desc")
    List<RepairRequest> findByCustomerIdAndStatusIn(Long userId, List<RepairStatus> statuses);

    @Query("select r from RepairRequest r where r.worker.id = :workerId and r.status in :statuses order by r.createdAt desc")
    List<RepairRequest> findByWorkerIdAndStatusIn(Long workerId, List<RepairStatus> statuses);

    @Query("select r from RepairRequest r where r.status = :status")
    List<RepairRequest> findByStatus(RepairStatus status);

    @Query("select r from RepairRequest r where r.status = :status")
    Page<RepairRequest> findByStatus(RepairStatus status, Pageable pageable);

    @Query("select r from RepairRequest r join fetch r.category join fetch r.customer " +
            "where r.worker.id = :workerId and r.status in :statuses order by r.createdAt asc")
    List<RepairRequest> findByWorkerAndStatusIn(@Param("workerId") Long workerId,
                                                @Param("statuses") java.util.Collection<com.thonha.backend.enums.RepairStatus> statuses);

    @Query("select r from RepairRequest r join fetch r.category join fetch r.customer " +
            "where r.worker.id = :workerId order by r.createdAt desc")
    List<RepairRequest> findRecentByWorker(@Param("workerId") Long workerId, Pageable pageable);

    @Query("select r from RepairRequest r where r.worker.id = :workerId and r.status = :status and r.updatedAt >= :since")
    List<RepairRequest> findByWorkerAndStatusSince(@Param("workerId") Long workerId,
                                                   @Param("status") RepairStatus status,
                                                   @Param("since") java.time.LocalDateTime since);


    @Query("select r.status, count(r) from RepairRequest r " +
            "where r.createdAt >= :from and r.createdAt < :to group by r.status")
    List<Object[]> countByStatusBetween(@Param("from") java.time.LocalDateTime from,
                                        @Param("to") java.time.LocalDateTime to);

    @Query("select r.category.name, count(r) from RepairRequest r " +
            "where r.createdAt >= :from and r.createdAt < :to " +
            "group by r.category.name order by count(r) desc")
    List<Object[]> topCategoriesBetween(@Param("from") java.time.LocalDateTime from,
                                        @Param("to") java.time.LocalDateTime to,
                                        Pageable pageable);

    @Query("select r.category.id, count(r) from RepairRequest r where r.category.id in :ids group by r.category.id")
    List<Object[]> countByCategoryIds(@Param("ids") java.util.Collection<Long> ids);

    @Query("select r.createdAt from RepairRequest r where r.createdAt >= :from and r.createdAt < :to")
    List<java.time.LocalDateTime> findCreatedAtBetween(@Param("from") java.time.LocalDateTime from,
                                                      @Param("to") java.time.LocalDateTime to);

    boolean existsByIdAndCustomerId(Long id, Long customerId);

    boolean existsByIdAndWorker_User_Id(Long id, Long userId);

    @Query("select r from RepairRequest r join fetch r.category where r.worker.user.id = :userId and r.status in :statuses order by r.createdAt desc")
    List<RepairRequest> findActiveByWorkerUser(@Param("userId") Long userId,
                                               @Param("statuses") java.util.Collection<RepairStatus> statuses);


    @Query("select r from RepairRequest r join fetch r.category left join fetch r.worker w left join fetch w.user " +
            "where r.customer.id = :userId and r.status = com.thonha.backend.enums.RepairStatus.COMPLETED " +
            "and r.completedAt >= :since " +
            "and not exists (select 1 from Review rv where rv.request.id = r.id) " +
            "order by r.completedAt desc")
    List<RepairRequest> findAwaitingReview(@Param("userId") Long userId,
                                           @Param("since") LocalDateTime since,
                                           Pageable pageable);
}
