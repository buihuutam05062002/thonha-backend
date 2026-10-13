package com.thonha.backend.repository;

import com.thonha.backend.entity.Complaint;
import com.thonha.backend.enums.ComplaintStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    @Override
    @EntityGraph(attributePaths = {"request", "request.customer", "request.worker", "request.worker.user",
            "complainant", "handledBy"})
    Page<Complaint> findAll(Specification<Complaint> spec, Pageable pageable);

    @Query("select c from Complaint c join fetch c.request r join fetch r.customer join fetch r.category " +
            "left join fetch r.worker w left join fetch w.user " +
            "join fetch c.complainant left join fetch c.handledBy where c.id = :id")
    Optional<Complaint> findByIdWithDetails(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Complaint c where c.id = :id")
    Optional<Complaint> findByIdForUpdate(@Param("id") Long id);

    boolean existsByRequestIdAndComplainantIdAndStatusIn(Long requestId, Long complainantId,
                                                         Collection<ComplaintStatus> statuses);

    @Query("select c from Complaint c join fetch c.request r join fetch r.customer " +
            "left join fetch r.worker w left join fetch w.user " +
            "join fetch c.complainant left join fetch c.handledBy " +
            "where c.complainant.id = :userId order by c.createdAt desc")
    List<Complaint> findByComplainant(@Param("userId") Long userId);

    @Query("select c.status, count(c) from Complaint c group by c.status")
    List<Object[]> countByStatus();
}
