package com.thonha.backend.service;

import com.thonha.backend.dto.request.CategoryCreateRequest;
import com.thonha.backend.dto.request.CategoryUpdateRequest;
import com.thonha.backend.dto.response.CategoryResponse;
import com.thonha.backend.enums.CategoryStatus;
import com.thonha.backend.entity.ServiceCategory;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.ServiceCategoryRepository;
import com.thonha.backend.dto.response.AdminCategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final ServiceCategoryRepository serviceCategoryRepository;

    private final RepairRequestRepository repairRequestRepository;

    public CategoryService(ServiceCategoryRepository serviceCategoryRepository,
                           RepairRequestRepository repairRequestRepository) {
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.repairRequestRepository = repairRequestRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminCategoryResponse> getAllForAdmin(Pageable pageable) {
        Page<ServiceCategory> page = serviceCategoryRepository.findAll(pageable);
        Map<Long, Long> counts = new HashMap<>();
        if (page.hasContent()) {
            List<Long> ids = page.getContent().stream().map(ServiceCategory::getId).toList();
            for (Object[] row : repairRequestRepository.countByCategoryIds(ids)) {
                counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
        }
        return page.map(c -> new AdminCategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getIcon(),
                c.getStatus(), counts.getOrDefault(c.getId(), 0L)));
    }

    public List<CategoryResponse> getAll() {
        return serviceCategoryRepository.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE)
                .stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    public CategoryResponse getById(Long id) {
        return serviceCategoryRepository.findById(id)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        com.thonha.backend.common.ErrorCode.NOT_FOUND,
                        "Không tìm thấy danh mục"));
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        if (serviceCategoryRepository.existsByName(request.getName())) {
            throw new com.thonha.backend.common.ApiException(
                    com.thonha.backend.common.ErrorCode.DUPLICATE_ENTRY,
                    "Tên danh mục đã tồn tại");
        }
        ServiceCategory category = ServiceCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .status(CategoryStatus.ACTIVE)
                .build();
        return CategoryResponse.from(serviceCategoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryUpdateRequest request) {
        ServiceCategory category = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new com.thonha.backend.common.ApiException(
                        com.thonha.backend.common.ErrorCode.NOT_FOUND,
                        "Không tìm thấy danh mục"));

        if (request.getName() != null && !request.getName().equals(category.getName())
                && serviceCategoryRepository.existsByName(request.getName())) {
            throw new com.thonha.backend.common.ApiException(
                    com.thonha.backend.common.ErrorCode.DUPLICATE_ENTRY,
                    "Tên danh mục đã tồn tại");
        }

        if (request.getName() != null) category.setName(request.getName());
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getIcon() != null) category.setIcon(request.getIcon());
        if (request.getStatus() != null) category.setStatus(request.getStatus());

        return CategoryResponse.from(serviceCategoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        if (!serviceCategoryRepository.existsById(id)) {
            throw new com.thonha.backend.common.ApiException(
                    com.thonha.backend.common.ErrorCode.NOT_FOUND,
                    "Không tìm thấy danh mục");
        }
        serviceCategoryRepository.deleteById(id);
    }
}