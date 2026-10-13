package com.thonha.backend.controller;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.ai.IncidentSuggestionResponse;
import com.thonha.backend.service.IncidentClassifierService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final IncidentClassifierService classifier;

    public AiController(IncidentClassifierService classifier) {
        this.classifier = classifier;
    }

    /** Phân loại sự cố từ mô tả + ảnh (multipart: "description", "files"). Cần đăng nhập. */
    @PostMapping(value = "/classify-incident", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<IncidentSuggestionResponse>> classifyIncident(
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.success(classifier.classify(description, files)));
    }
}
