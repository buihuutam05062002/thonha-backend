package com.thonha.backend.controller.worker;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.ScheduleSlotRequest;
import com.thonha.backend.dto.response.ScheduleSlotResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.WorkerScheduleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/worker/schedule")
@PreAuthorize("hasRole('WORKER')")
public class WorkerScheduleController {

    private final WorkerScheduleService scheduleService;
    private final CurrentUserProvider currentUserProvider;

    public WorkerScheduleController(WorkerScheduleService scheduleService, CurrentUserProvider currentUserProvider) {
        this.scheduleService = scheduleService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleSlotResponse>>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.list(currentUserProvider.requireUserId(), from, to)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleSlotResponse>> create(@Valid @RequestBody ScheduleSlotRequest body) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                scheduleService.create(currentUserProvider.requireUserId(), body)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleSlotResponse>> update(@PathVariable Long id,
                                                                   @Valid @RequestBody ScheduleSlotRequest body) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.update(currentUserProvider.requireUserId(), id, body)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        scheduleService.delete(currentUserProvider.requireUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa khung giờ"));
    }
}
