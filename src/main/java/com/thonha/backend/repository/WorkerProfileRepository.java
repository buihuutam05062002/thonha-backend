package com.thonha.backend.repository;

import com.thonha.backend.entity.*;
import com.thonha.backend.enums.ApprovalStatus;
import com.thonha.backend.enums.AvailabilityStatus;

import java.util.*;
import java.util.List;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, Long> {
    Optional<WorkerProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);


    @EntityGraph(attributePaths = {"user", "reviewedBy", "bankAccount"})
    Optional<WorkerProfile> findWithDetailById(Long id);

    @EntityGraph(attributePaths = {"user", "reviewedBy"})
    @Query(value = "select w from WorkerProfile w join w.user u " +
            "where (:status is null or w.approvalStatus = :status) " +
            "and (:keyword is null or lower(u.fullName) like lower(concat('%', :keyword, '%')) " +
            "     or lower(u.email) like lower(concat('%', :keyword, '%')) " +
            "     or u.phoneNumber like concat('%', :keyword, '%')) " +
            "and (:city is null or lower(w.provinceCity) like lower(concat('%', :city, '%')))",
            countQuery = "select count(w) from WorkerProfile w join w.user u " +
            "where (:status is null or w.approvalStatus = :status) " +
            "and (:keyword is null or lower(u.fullName) like lower(concat('%', :keyword, '%')) " +
            "     or lower(u.email) like lower(concat('%', :keyword, '%')) " +
            "     or u.phoneNumber like concat('%', :keyword, '%')) " +
            "and (:city is null or lower(w.provinceCity) like lower(concat('%', :city, '%')))")
    Page<WorkerProfile> search(@Param("status") ApprovalStatus status, @Param("keyword") String keyword,
                               @Param("city") String city, Pageable pageable);

    long countByApprovalStatus(ApprovalStatus status);

    // Tìm thợ khả dụng để matching
    @Query("select w from WorkerProfile w " +
            "join w.user u " +
            "join w.specialties s " +
            "where w.approvalStatus = :approvalStatus " +
            "and w.availabilityStatus = :availabilityStatus " +
            "and u.status = com.thonha.backend.enums.UserStatus.ACTIVE " +
            "and s.serviceCategory.id = :categoryId " +
            "and w.ongoingJobsCount < 3 " +
            "and (:city is null or lower(w.provinceCity) like lower(concat('%',:city,'%'))) ")
    List<WorkerProfile> findAvailableWorkers(
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("availabilityStatus") AvailabilityStatus availabilityStatus,
            @Param("categoryId") Long categoryId,
            @Param("city") String city);

    // Ghép thợ: chỉ xét các thợ trong danh sách id (đã lọc theo vị trí GPS từ Redis)
    @Query("select distinct w from WorkerProfile w " +
            "join fetch w.user u " +
            "join w.specialties s " +
            "where w.id in :ids " +
            "and w.approvalStatus = :approvalStatus " +
            "and w.availabilityStatus = :availabilityStatus " +
            "and u.status = com.thonha.backend.enums.UserStatus.ACTIVE " +
            "and s.serviceCategory.id = :categoryId " +
            "and u.id <> :excludedUserId " +   // không ghép khách với chính tài khoản của họ (vừa là khách vừa là thợ)
            "and not exists (select 1 from Wallet wl where wl.worker = w and wl.balance < :minBalance) " +   // ví dưới mức tối thiểu thì không nhận đơn
            "and w.ongoingJobsCount < 3")
    List<WorkerProfile> findAvailableWorkersByIds(
            @Param("ids") java.util.Collection<Long> ids,
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("availabilityStatus") AvailabilityStatus availabilityStatus,
            @Param("categoryId") Long categoryId,
            @Param("excludedUserId") Long excludedUserId,
            @Param("minBalance") java.math.BigDecimal minBalance);

    // ---- Số đơn đang làm: cập nhật nguyên tử bằng SQL để tránh mất cập nhật khi nhiều đơn được nhận cùng lúc.
    // (Không dùng clearAutomatically: sẽ làm các entity khác trong cùng transaction bị detach.)

    @Modifying(flushAutomatically = true)
    @Query("update WorkerProfile w set w.ongoingJobsCount = w.ongoingJobsCount + 1 " +
            "where w.id = :id and w.ongoingJobsCount < :max")
    int tryIncrementOngoing(@Param("id") Long id, @Param("max") int max);

    @Modifying(flushAutomatically = true)
    @Query("update WorkerProfile w set w.ongoingJobsCount = w.ongoingJobsCount - 1 " +
            "where w.id = :id and w.ongoingJobsCount > 0")
    int decrementOngoing(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("update WorkerProfile w set w.availabilityStatus = com.thonha.backend.enums.AvailabilityStatus.BUSY " +
            "where w.id = :id and w.availabilityStatus = com.thonha.backend.enums.AvailabilityStatus.READY " +
            "and w.ongoingJobsCount >= :max")
    int markBusyIfFull(@Param("id") Long id, @Param("max") int max);

    @Modifying(flushAutomatically = true)
    @Query("update WorkerProfile w set w.availabilityStatus = com.thonha.backend.enums.AvailabilityStatus.READY " +
            "where w.id = :id and w.availabilityStatus = com.thonha.backend.enums.AvailabilityStatus.BUSY " +
            "and w.ongoingJobsCount < :max")
    int markReadyIfFree(@Param("id") Long id, @Param("max") int max);

    /** Khóa dòng hồ sơ thợ để serialize các thao tác thay đổi lịch của cùng một thợ. */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkerProfile w where w.id = :id")
    Optional<WorkerProfile> findByIdForUpdate(@Param("id") Long id);

    /** Mỗi dòng: [ApprovalStatus, count]. */
    @Query("select w.approvalStatus, count(w) from WorkerProfile w group by w.approvalStatus")
    List<Object[]> countByApprovalStatus();

    /** Số hồ sơ thợ nộp trong khoảng [from, to). */
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(java.time.LocalDateTime from, java.time.LocalDateTime to);
}
