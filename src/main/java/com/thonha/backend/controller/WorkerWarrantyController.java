package com.thonha.backend.controller;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.AcceptWarrantyRequest;
import com.thonha.backend.dto.request.CompleteWarrantyRequest;
import com.thonha.backend.dto.request.RejectWarrantyRequest;
import com.thonha.backend.dto.response.WarrantyClaimResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.WarrantyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/worker/warranties")
@RequiredArgsConstructor
public class WorkerWarrantyController {
    private final WarrantyService warrantyService;
    private final CurrentUserProvider currentUser;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarrantyClaimResponse>>> list(
            @RequestParam(defaultValue = "PENDING") String tab,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                warrantyService.listForWorker(currentUser.requireUserId(), tab, keyword)
        ));
    }

    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> counts() {
        return ResponseEntity.ok(ApiResponse.success(warrantyService.counts(currentUser.requireUserId())));
    }

    @GetMapping("/{claimId}")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> detail(@PathVariable Long claimId) {
        return ResponseEntity.ok(ApiResponse.success(
                warrantyService.detail(currentUser.requireUserId(), claimId)));
    }

    @PostMapping("/{claimId}/reject")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> reject(
            @PathVariable Long claimId, @Valid @RequestBody RejectWarrantyRequest body) {
        return ResponseEntity.ok(ApiResponse.success(
                warrantyService.reject(currentUser.requireUserId(), claimId, body), "Đã từ chối yêu cầu bảo hành"));
    }

    @PostMapping("/{claimId}/accept")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> accept(
            @PathVariable Long claimId, @Valid @RequestBody AcceptWarrantyRequest body) {
        return ResponseEntity.ok(ApiResponse.success(
                warrantyService.accept(currentUser.requireUserId(), claimId, body), "Đã gửi lịch hẹn cho khách"));
    }

    @PostMapping(value = "/{claimId}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> complete(
            @PathVariable Long claimId,
            @Valid @RequestPart("data") CompleteWarrantyRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.success(
                warrantyService.complete(currentUser.requireUserId(), claimId, data, files),
                "Đã nghiệm thu bảo hành"));
    }
}
