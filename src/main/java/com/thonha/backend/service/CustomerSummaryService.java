package com.thonha.backend.service;

import com.thonha.backend.dto.response.CustomerSummaryResponse;
import com.thonha.backend.dto.response.CustomerSummaryResponse.AwaitingReview;
import com.thonha.backend.dto.response.CustomerSummaryResponse.PendingQuotation;
import com.thonha.backend.enums.QuotationStatus;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.repository.QuotationRepository;
import com.thonha.backend.repository.RepairRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerSummaryService {
    private static final int LIMIT = 5;
    private static final int REVIEW_WINDOW_DAYS = 30;
    private static final List<RepairStatus> TERMINAL =
            List.of(RepairStatus.COMPLETED, RepairStatus.CANCELLED, RepairStatus.NOT_FOUND);

    private final QuotationRepository quotationRepository;
    private final RepairRequestRepository repairRequestRepository;

    @Transactional(readOnly = true)
    public CustomerSummaryResponse summarize(Long userId) {
        List<PendingQuotation> pending = quotationRepository
                .findForCustomerByStatus(userId, QuotationStatus.PENDING, TERMINAL, PageRequest.of(0, LIMIT))
                .stream()
                .map(q -> new PendingQuotation(
                        q.getId(), q.getRequest().getId(), q.getRequest().getRequestCode(),
                        q.getRequest().getCategory().getName(),
                        q.getWorker().getUser().getFullName(),
                        q.getTotalAmount(), q.getCreatedAt()))
                .toList();

        List<AwaitingReview> awaiting = repairRequestRepository
                .findAwaitingReview(userId, LocalDateTime.now().minusDays(REVIEW_WINDOW_DAYS),
                        PageRequest.of(0, LIMIT))
                .stream()
                .map(r -> new AwaitingReview(
                        r.getId(), r.getRequestCode(), r.getCategory().getName(),
                        r.getWorker() == null ? null : r.getWorker().getUser().getFullName(),
                        r.getCompletedAt()))
                .toList();

        return new CustomerSummaryResponse(pending, awaiting);
    }
}