package com.thonha.backend.controller.admin;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.request.CategoryCreateRequest;
import com.thonha.backend.dto.request.CategoryUpdateRequest;
import com.thonha.backend.dto.response.CategoryResponse;
import com.thonha.backend.enums.CategoryStatus;
import com.thonha.backend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.thonha.backend.dto.response.AdminCategoryResponse;

@RestController
@RequestMapping("/api/v1/admin/service-categories")
public class CategoryAdminController {
    private final CategoryService categoryService;

    public CategoryAdminController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Page<AdminCategoryResponse> list(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return categoryService.getAllForAdmin(pageable);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(categoryService.create(request), "Tạo danh mục thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.update(id, request), "Cập nhật danh mục thành công"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CategoryResponse>> status(@PathVariable Long id,
                                                                @RequestParam CategoryStatus status) {
        CategoryUpdateRequest request = CategoryUpdateRequest.builder().status(status).build();
        return ResponseEntity.ok(ApiResponse.success(categoryService.update(id, request), "Cập nhật trạng thái thành công"));
    }
}