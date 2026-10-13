package com.thonha.backend.repository;

import com.thonha.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    long countByWorkerId(Long workerId);

    @Query("select r.worker.id, avg(r.rating), count(r) from Review r " +
            "where r.worker.id in :workerIds group by r.worker.id")
    List<Object[]> summarizeByWorkers(@Param("workerIds") Collection<Long> workerIds);

    @EntityGraph(attributePaths = {"customer", "request"})
    Page<Review> findByWorkerId(Long workerId, Pageable pageable);

    @Query("select r.rating, count(r) from Review r where r.worker.id = :workerId group by r.rating")
    List<Object[]> countByRatingForWorker(@Param("workerId") Long workerId);

    boolean existsByRequestId(Long requestId);

    @Query("select r from Review r join fetch r.customer join fetch r.worker w join fetch w.user where r.request.id = :requestId")
    java.util.Optional<Review> findByRequestIdWithDetails(@Param("requestId") Long requestId);

    @Query("select avg(r.rating), count(r) from Review r where r.createdAt >= :from and r.createdAt < :to")
    List<Object[]> summarizeBetween(@Param("from") java.time.LocalDateTime from,
                                    @Param("to") java.time.LocalDateTime to);
}
