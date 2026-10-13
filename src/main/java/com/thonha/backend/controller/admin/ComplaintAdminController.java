package com.thonha.backend.controller.admin;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.ResolveComplaintRequest;
import com.thonha.backend.dto.response.AdminComplaintDetailResponse;
import com.thonha.backend.dto.response.ComplaintResponse;
import com.thonha.backend.enums.ComplaintReason;
import com.thonha.backend.enums.ComplaintStatus;
import com.thonha.backend.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/complaints")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ComplaintAdminController {

    private final ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ComplaintResponse>>> list(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintReason reason,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), clamp(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(complaintService.search(status, reason, keyword, pageable)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        return ResponseEntity.ok(ApiResponse.success(complaintService.stats()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminComplaintDetailResponse>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(complaintService.detail(id)));
    }

    @PostMapping("/{id}/take")
    public ResponseEntity<ApiResponse<ComplaintResponse>> take(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(complaintService.take(id), "Đã nhận xử lý khiếu nại"));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<ComplaintResponse>> resolve(
            @PathVariable Long id, @Valid @RequestBody ResolveComplaintRequest body) {
        return ResponseEntity.ok(ApiResponse.success(complaintService.resolve(id, body), "Đã kết luận khiếu nại"));
    }

    private static int clamp(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
