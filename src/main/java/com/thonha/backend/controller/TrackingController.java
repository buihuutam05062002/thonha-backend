package com.thonha.backend.controller;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.tracking.ActiveJob;
import com.thonha.backend.dto.tracking.TrackingSnapshot;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;
    private final CurrentUserProvider currentUserProvider;

    /** Khách (hoặc thợ được ghép) mở trang theo dõi: trạng thái, thông tin thợ và vị trí gần nhất. */
    @GetMapping("/repair-requests/{id}/tracking")
    public ResponseEntity<ApiResponse<TrackingSnapshot>> tracking(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                trackingService.snapshot(id, currentUserProvider.requireUserId())));
    }

    /** Thợ: các đơn đang thực hiện (đã ghép / đang đến) để chọn đơn chia sẻ vị trí. */
    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/worker/jobs/active")
    public ResponseEntity<ApiResponse<List<ActiveJob>>> activeJobs() {
        return ResponseEntity.ok(ApiResponse.success(
                trackingService.activeJobs(currentUserProvider.requireUserId())));
    }
}
