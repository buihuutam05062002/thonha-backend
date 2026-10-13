package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.UpsertCommissionPolicyRequest;
import com.thonha.backend.dto.response.CommissionPolicyResponse;
import com.thonha.backend.entity.CommissionPolicy;
import com.thonha.backend.entity.ServiceCategory;
import com.thonha.backend.repository.CommissionPolicyRepository;
import com.thonha.backend.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class CommissionPolicyService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CommissionPolicyRepository policyRepository;
    private final ServiceCategoryRepository categoryRepository;

    @Value("${app.commission.default-rate:10}")
    private BigDecimal configDefaultRate;

    @Transactional(readOnly = true)
    public BigDecimal resolveRatePercent(Long categoryId) {
        Optional<CommissionPolicy> byCategory = categoryId == null ? Optional.empty()
                : policyRepository.findByScopeKeyAndActiveTrue(CommissionPolicy.scopeOfCategory(categoryId));
        if (byCategory.isPresent()) {
            return byCategory.get().getRatePercent();
        }
        return policyRepository.findByScopeKeyAndActiveTrue(CommissionPolicy.DEFAULT_SCOPE)
                .map(CommissionPolicy::getRatePercent)
                .orElseGet(this::validatedConfigDefault);
    }

    @Transactional(readOnly = true)
    public CommissionPolicyResponse.Overview overview() {
        List<CommissionPolicy> all = policyRepository.findAllByOrderByIdAsc();
        Optional<CommissionPolicy> def = all.stream()
                .filter(p -> CommissionPolicy.DEFAULT_SCOPE.equals(p.getScopeKey()) && p.isActive())
                .findFirst();
        List<CommissionPolicyResponse> perCategory = all.stream()
                .filter(p -> p.getCategory() != null)
                .map(CommissionPolicyResponse::from)
                .toList();
        return new CommissionPolicyResponse.Overview(
                def.map(CommissionPolicy::getRatePercent).orElseGet(this::validatedConfigDefault),
                def.isEmpty(),
                perCategory);
    }

    @Transactional
    public CommissionPolicyResponse upsert(UpsertCommissionPolicyRequest request, Long adminUserId) {
        String scope;
        ServiceCategory category = null;
        if (request.getCategoryId() == null) {
            scope = CommissionPolicy.DEFAULT_SCOPE;
        } else {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));
            scope = CommissionPolicy.scopeOfCategory(category.getId());
        }

        CommissionPolicy policy = policyRepository.findByScopeKey(scope).orElse(null);
        if (policy == null) {
            policy = CommissionPolicy.builder().scopeKey(scope).category(category).build();
        }
        policy.setRatePercent(request.getRatePercent().setScale(2, RoundingMode.HALF_UP));
        policy.setActive(request.getActive() == null || request.getActive());
        policy.setUpdatedBy(adminUserId);
        try {
            return CommissionPolicyResponse.from(policyRepository.saveAndFlush(policy));
        } catch (DataIntegrityViolationException e) {
            // hai admin cùng tạo một chính sách cùng lúc: UNIQUE(scope_key) chặn bản thứ hai
            throw new ApiException(ErrorCode.CONFLICT, "Chính sách đang được người khác cập nhật, vui lòng thử lại");
        }
    }

    private BigDecimal validatedConfigDefault() {
        if (configDefaultRate == null || configDefaultRate.signum() < 0 || configDefaultRate.compareTo(HUNDRED) > 0) {
            throw new ApiException(ErrorCode.CONFIGURATION_ERROR,
                    "Cấu hình app.commission.default-rate phải trong khoảng 0 đến 100");
        }
        return configDefaultRate.setScale(2, RoundingMode.HALF_UP);
    }
}
