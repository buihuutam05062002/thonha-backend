package com.thonha.backend.controller;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.CreateWarrantyClaimRequest;
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

@RestController
@RequestMapping("/api/v1/warranty-claims")
@RequiredArgsConstructor
public class WarrantyClaimController {
    private final WarrantyService warrantyService;
    private final CurrentUserProvider currentUser;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> create(
            @Valid @RequestPart("data") CreateWarrantyClaimRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                warrantyService.createClaim(currentUser.requireUserId(),data,files),
                "Đã gửi yêu cầu bảo hành"
        ));
    }
    
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<WarrantyClaimResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.success(warrantyService.listMine(currentUser.requireUserId())));
    }
}
