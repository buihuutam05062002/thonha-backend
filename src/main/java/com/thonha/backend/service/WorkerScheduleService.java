package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.ScheduleSlotRequest;
import com.thonha.backend.dto.response.ScheduleSlotResponse;
import com.thonha.backend.entity.WorkerProfile;
import com.thonha.backend.entity.WorkerSchedule;
import com.thonha.backend.enums.ApprovalStatus;
import com.thonha.backend.enums.ScheduleStatus;
import com.thonha.backend.repository.WorkerProfileRepository;
import com.thonha.backend.repository.WorkerScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;


@Service
public class WorkerScheduleService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final int MAX_DAYS_AHEAD = 90;
    public static final int MAX_RANGE_DAYS = 62;
    public static final int MAX_SLOTS_PER_DAY = 20;

    private final WorkerScheduleRepository scheduleRepository;
    private final WorkerProfileRepository workerProfileRepository;

    public WorkerScheduleService(WorkerScheduleRepository scheduleRepository,
                                 WorkerProfileRepository workerProfileRepository) {
        this.scheduleRepository = scheduleRepository;
        this.workerProfileRepository = workerProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleSlotResponse> list(Long userId, LocalDate from, LocalDate to) {
        WorkerProfile profile = requireApprovedWorker(userId);
        if (from == null || to == null || to.isBefore(from)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Khoảng ngày không hợp lệ");
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Chỉ xem tối đa " + MAX_RANGE_DAYS + " ngày mỗi lần");
        }
        return scheduleRepository
                .findByWorkerProfileIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(profile.getId(), from, to)
                .stream().map(ScheduleSlotResponse::from).toList();
    }

    @Transactional
    public ScheduleSlotResponse create(Long userId, ScheduleSlotRequest body) {
        WorkerProfile profile = requireApprovedWorker(userId);
        workerProfileRepository.findByIdForUpdate(profile.getId());
        ScheduleStatus status = validate(profile.getId(), body, null);
        if (scheduleRepository.countByWorkerProfileIdAndWorkDate(profile.getId(), body.getWorkDate()) >= MAX_SLOTS_PER_DAY) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Mỗi ngày tối đa " + MAX_SLOTS_PER_DAY + " khung giờ");
        }
        WorkerSchedule saved = scheduleRepository.save(WorkerSchedule.builder()
                .workerProfile(profile)
                .workDate(body.getWorkDate())
                .startTime(body.getStartTime())
                .endTime(body.getEndTime())
                .status(status)
                .build());
        return ScheduleSlotResponse.from(saved);
    }

    @Transactional
    public ScheduleSlotResponse update(Long userId, Long slotId, ScheduleSlotRequest body) {
        WorkerProfile profile = requireApprovedWorker(userId);
        workerProfileRepository.findByIdForUpdate(profile.getId());
        WorkerSchedule slot = requireOwnedEditable(profile.getId(), slotId, "sửa");
        ScheduleStatus status = validate(profile.getId(), body, slot.getId());
        slot.setWorkDate(body.getWorkDate());
        slot.setStartTime(body.getStartTime());
        slot.setEndTime(body.getEndTime());
        slot.setStatus(status);
        return ScheduleSlotResponse.from(scheduleRepository.save(slot));
    }

    @Transactional
    public void delete(Long userId, Long slotId) {
        WorkerProfile profile = requireApprovedWorker(userId);
        scheduleRepository.delete(requireOwnedEditable(profile.getId(), slotId, "xóa"));
    }


    private WorkerProfile requireApprovedWorker(Long userId) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
        if (profile.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ApiException(ErrorCode.WORKER_NOT_APPROVED);
        }
        return profile;
    }

    private WorkerSchedule requireOwnedEditable(Long workerId, Long slotId, String action) {
        WorkerSchedule slot = scheduleRepository.findByIdAndWorkerProfileId(slotId, workerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy khung giờ"));
        if (slot.getStatus() == ScheduleStatus.BOOKED) {
            throw new ApiException(ErrorCode.UNPROCESSABLE_ENTITY, "Khung giờ đã có đơn đặt, không thể " + action);
        }
        return slot;
    }

    private ScheduleStatus validate(Long workerId, ScheduleSlotRequest b, Long excludeId) {
        ScheduleStatus status = b.getStatus() == null ? ScheduleStatus.AVAILABLE : b.getStatus();
        if (status == ScheduleStatus.BOOKED) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Trạng thái \"Đã đặt\" do hệ thống quản lý");
        }
        if (!b.getEndTime().isAfter(b.getStartTime())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Giờ kết thúc phải sau giờ bắt đầu");
        }
        LocalDate today = LocalDate.now(ZONE);
        if (b.getWorkDate().isBefore(today)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Không thể đặt lịch cho ngày đã qua");
        }
        if (b.getWorkDate().isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Chỉ đặt lịch trước tối đa " + MAX_DAYS_AHEAD + " ngày");
        }
        if (scheduleRepository.existsOverlap(workerId, b.getWorkDate(), b.getStartTime(), b.getEndTime(), excludeId)) {
            throw new ApiException(ErrorCode.CONFLICT, "Khung giờ này trùng với một khung giờ đã có trong ngày");
        }
        return status;
    }
}
