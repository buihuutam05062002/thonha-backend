package com.thonha.backend.controller;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.CreateRepairRequest;
import com.thonha.backend.dto.response.RepairRequestResponse;
import com.thonha.backend.enums.MatchingResult;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.MatchingService;
import com.thonha.backend.service.RepairRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/repair-requests")
public class RepairRequestController {
    private final RepairRequestService repairRequestService;
    private final CurrentUserProvider currentUserProvider;
    private final MatchingService matchingService;

    public RepairRequestController(RepairRequestService repairRequestService,
                                   CurrentUserProvider currentUserProvider,
                                   MatchingService matchingService) {
        this.repairRequestService = repairRequestService;
        this.currentUserProvider = currentUserProvider;
        this.matchingService = matchingService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RepairRequestResponse>> create(
            @Valid @RequestPart("data") CreateRepairRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        Long userId = currentUserProvider.requireUserId();
        ApiResponse<RepairRequestResponse> response = repairRequestService.create(userId, data, files);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RepairRequestResponse>>> getMyRequests() {
        return ResponseEntity.ok(repairRequestService.getMyRequests(currentUserProvider.requireUserId()));
    }


    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<RepairRequestResponse>>> searchMyRequests(
            @RequestParam(required = false) List<RepairStatus> status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(repairRequestService.searchMyRequests(
                currentUserProvider.requireUserId(), status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RepairRequestResponse>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(repairRequestService.getDetail(id, currentUserProvider.requireUserId()));
    }

    @GetMapping("/{id}/worker-location")
    public ResponseEntity<ApiResponse<com.thonha.backend.dto.response.WorkerLocationResponse>> workerLocation(@PathVariable Long id) {
        return ResponseEntity.ok(repairRequestService.getWorkerLocation(currentUserProvider.requireUserId(), id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RepairRequestResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(repairRequestService.cancelRequest(currentUserProvider.requireUserId(), id));
    }

    @PostMapping("/{id}/retry-matching")
    public ResponseEntity<ApiResponse<MatchingService.MatchingResultDTO>> retryMatching(@PathVariable Long id) {
        Long userId = currentUserProvider.requireUserId();
        repairRequestService.assertOwner(id, userId); // chỉ chủ yêu cầu mới được tìm lại thợ
        MatchingService.MatchingResultDTO result = matchingService.findAndMatchWorkers(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/matching-response")
    public ResponseEntity<ApiResponse<Void>> matchingResponse(
            @PathVariable Long id,
            @RequestBody MatchingResponseRequest request) {
        matchingService.handleWorkerResponse(request.getMatchingLogId(), currentUserProvider.requireUserId(), request.getResult(), request.getNote());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @lombok.Data
    public static class MatchingResponseRequest {
        private Long matchingLogId;
        private MatchingResult result;
        private String note;
    }
}