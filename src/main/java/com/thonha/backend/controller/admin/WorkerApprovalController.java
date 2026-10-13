package com.thonha.backend.controller.admin;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.RejectWorkerRequest;
import com.thonha.backend.dto.request.WorkerProfileSearchRequest;
import com.thonha.backend.dto.admin.WorkerApprovalResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.WorkerApprovalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/worker-profiles")
public class WorkerApprovalController {

    private final WorkerApprovalService workerApprovalService;
    private final CurrentUserProvider currentUserProvider;

    public WorkerApprovalController(WorkerApprovalService workerApprovalService, CurrentUserProvider currentUserProvider) {
        this.workerApprovalService = workerApprovalService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public com.thonha.backend.common.ApiResponse<org.springframework.data.domain.Page<WorkerApprovalResponse>> search(
            WorkerProfileSearchRequest request, Pageable pageable) {
        return com.thonha.backend.common.ApiResponse.success(
                workerApprovalService.search(request, pageable)
        );
    }

    @GetMapping("/stats")
    public com.thonha.backend.common.ApiResponse<WorkerApprovalResponse.Stats> stats() {
        return com.thonha.backend.common.ApiResponse.success(workerApprovalService.stats());
    }

    @GetMapping("/{id}")
    public com.thonha.backend.common.ApiResponse<WorkerApprovalResponse> getDetail(@PathVariable Long id) {
        return com.thonha.backend.common.ApiResponse.success(workerApprovalService.getDetail(id));
    }

    @PatchMapping("/{id}/approve")
    public com.thonha.backend.common.ApiResponse<WorkerApprovalResponse> approve(@PathVariable Long id) {
        Long adminId = requireAdminId();
        return com.thonha.backend.common.ApiResponse.success(
                workerApprovalService.approve(id, adminId), "Duyệt hồ sơ thợ thành công"
        );
    }

    @PatchMapping("/{id}/reject")
    public com.thonha.backend.common.ApiResponse<WorkerApprovalResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectWorkerRequest request) {
        Long adminId = requireAdminId();
        return com.thonha.backend.common.ApiResponse.success(
                workerApprovalService.reject(id, adminId, request),
                "Từ chối hồ sơ thợ thành công"
        );
    }

    private Long requireAdminId() {
        Long userId = currentUserProvider.requireUserId();
        return userId;
    }
}