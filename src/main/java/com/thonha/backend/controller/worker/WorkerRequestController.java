package com.thonha.backend.controller.worker;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.UpdateRequestStatusRequest;
import com.thonha.backend.dto.response.RepairRequestResponse;
import com.thonha.backend.dto.worker.WorkerOrderItemResponse;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.RepairRequestService;
import com.thonha.backend.service.WorkerEarningsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/worker/requests")
@PreAuthorize("hasRole('WORKER')")
@RequiredArgsConstructor
public class WorkerRequestController {

    private final RepairRequestService repairRequestService;
    private final WorkerEarningsService workerEarningsService;
    private final CurrentUserProvider currentUserProvider;


    @GetMapping
    public ResponseEntity<ApiResponse<Page<WorkerOrderItemResponse>>> list(
            @RequestParam(required = false) List<RepairStatus> status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                workerEarningsService.orders(currentUserProvider.requireUserId(), status, keyword, page, size)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RepairRequestResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequestStatusRequest body) {
        return ResponseEntity.ok(
                repairRequestService.updateStatusByWorker(currentUserProvider.requireUserId(), id, body.getStatus(), body.getFinalPrice()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RepairRequestResponse>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(repairRequestService.getDetailForWorker(currentUserProvider.requireUserId(), id));
    }
}
