package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.CreateReviewRequest;
import com.thonha.backend.dto.response.ReviewResponse;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.entity.Review;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.ReviewRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewService {

    ReviewRepository reviewRepository;
    RepairRequestRepository repairRequestRepository;
    NotificationService notificationService;



    @Transactional
    public ReviewResponse create(Long customerUserId, CreateReviewRequest body) {
        RepairRequest request = repairRequestRepository.findByIdForUpdate(body.getRequestId())
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND));
        if (!request.getCustomer().getId().equals(customerUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Bạn không phải chủ của yêu cầu này");
        }
        if (request.getStatus() != RepairStatus.COMPLETED || request.getWorker() == null) {
            throw new ApiException(ErrorCode.UNPROCESSABLE_ENTITY, "Chỉ đánh giá được yêu cầu đã hoàn thành");
        }
        if (reviewRepository.existsByRequestId(request.getId())) {
            throw new ApiException(ErrorCode.CONFLICT, "Bạn đã đánh giá yêu cầu này rồi");
        }

        String comment = body.getComment() == null || body.getComment().isBlank() ? null : body.getComment().trim();
        Review saved = reviewRepository.saveAndFlush(Review.builder()
                .request(request)
                .customer(request.getCustomer())
                .worker(request.getWorker())
                .rating(body.getRating())
                .comment(comment)
                .build());

        notificationService.sendReviewReceived(request.getWorker().getUser().getId(), request.getId(), body.getRating());
        return ReviewResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getByRequest(Long userId, Long requestId) {
        RepairRequest request = repairRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND));
        boolean isCustomer = userId.equals(request.getCustomer().getId());
        boolean isWorker = request.getWorker() != null && userId.equals(request.getWorker().getUser().getId());
        if (!isCustomer && !isWorker) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Bạn không có quyền xem đánh giá của yêu cầu này");
        }
        return reviewRepository.findByRequestIdWithDetails(requestId).map(ReviewResponse::from).orElse(null);
    }
}
