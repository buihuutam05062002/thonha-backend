package com.thonha.backend.repository;

import com.thonha.backend.entity.WorkerSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface WorkerScheduleRepository extends JpaRepository<WorkerSchedule, Long> {

    List<WorkerSchedule> findByWorkerProfileIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(
            Long workerProfileId, LocalDate from, LocalDate to);

    Optional<WorkerSchedule> findByIdAndWorkerProfileId(Long id, Long workerProfileId);

    long countByWorkerProfileIdAndWorkDate(Long workerProfileId, LocalDate workDate);

    /** Có khung giờ nào của thợ trong ngày chồng lên [start, end) không (bỏ qua excludeId khi sửa). */
    @Query("select count(s) > 0 from WorkerSchedule s where s.workerProfile.id = :workerId and s.workDate = :date " +
            "and s.startTime < :end and s.endTime > :start and (:excludeId is null or s.id <> :excludeId)")
    boolean existsOverlap(@Param("workerId") Long workerId, @Param("date") LocalDate date,
                          @Param("start") LocalTime start, @Param("end") LocalTime end,
                          @Param("excludeId") Long excludeId);
}
