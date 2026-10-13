package com.thonha.backend.repository;

import com.thonha.backend.entity.MatchingLog;
import com.thonha.backend.enums.MatchingResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchingLogRepository extends JpaRepository<MatchingLog, Long> {
    Optional<MatchingLog> findByRequestIdAndWorkerId(Long requestId, Long workerId);

    List<MatchingLog> findByRequestIdOrderBySentAtDesc(Long requestId);

    List<MatchingLog> findByWorkerIdAndResultIn(Long workerId, List<MatchingResult> results);

    @Query("select m from MatchingLog m where m.request.id = :requestId and m.result = :result")
    Optional<MatchingLog> findByRequestIdAndResult(Long requestId, MatchingResult result);

    @Query("select m from MatchingLog m where m.worker.id = :workerId and m.result = :result and m.sentAt >= :since")
    List<MatchingLog> findRecentByWorkerAndResult(Long workerId, MatchingResult result, LocalDateTime since);

    @Query("select m from MatchingLog m where m.request.id = :requestId and m.result = :result")
    List<MatchingLog> findByRequestIdAndResultList(Long requestId, MatchingResult result);

    Page<MatchingLog> findByRequestId(Long requestId, Pageable pageable);

    long countByRequestIdAndResult(Long requestId, MatchingResult result);

    @Query("select m.worker.id, m.result, count(m) from MatchingLog m " +
            "where m.worker.id in :workerIds and m.result in :results and m.sentAt >= :since " +
            "group by m.worker.id, m.result")
    List<Object[]> countResponsesByWorkers(@Param("workerIds") java.util.Collection<Long> workerIds,
                                           @Param("results") java.util.Collection<MatchingResult> results,
                                           @Param("since") LocalDateTime since);

    @Query("select count(m) from MatchingLog m where m.request.id = :requestId and m.result in :results")
    long countByRequestIdAndResultIn(Long requestId, List<MatchingResult> results);

    @Query("select count(m) from MatchingLog m where m.worker.id = :workerId and m.result = :result and m.sentAt >= :since")
    long countRecentRejectionsByWorker(Long workerId, MatchingResult result, LocalDateTime since);

    @Query("select m from MatchingLog m where m.result = :result and m.sentAt < :dateTime")
    List<MatchingLog> findByResultAndSentAtBefore(MatchingResult result, LocalDateTime dateTime);

    @Query("select m from MatchingLog m join fetch m.request r join fetch r.category join fetch r.customer " +
            "where m.worker.id = :workerId and m.result = :result and m.sentAt >= :since order by m.sentAt desc")
    List<MatchingLog> findPendingForWorker(@Param("workerId") Long workerId,
                                           @Param("result") MatchingResult result,
                                           @Param("since") LocalDateTime since);
}
