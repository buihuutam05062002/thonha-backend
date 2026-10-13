package com.thonha.backend.controller;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.CreateComplaintRequest;
import com.thonha.backend.dto.response.ComplaintResponse;
import com.thonha.backend.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ComplaintResponse>> create(
            @Valid @RequestPart("data") CreateComplaintRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                complaintService.create(data, files), "Đã gửi khiếu nại, chúng tôi sẽ xử lý sớm nhất"));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> mine(
            @RequestParam(required = false) Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(complaintService.mine(requestId)));
    }
}
