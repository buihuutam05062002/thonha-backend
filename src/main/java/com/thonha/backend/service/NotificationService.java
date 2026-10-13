package com.thonha.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
public class NotificationService {

    private SimpMessagingTemplate messagingTemplate;

    public NotificationService() {}

    @Autowired(required = false)
    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    private void push(String destination, MatchingNotification notification) {
        if (messagingTemplate == null) {
            log.debug("WebSocket not enabled, skipping {} -> {}", notification.getType(), destination);
            return;
        }
        Runnable send = () -> {
            try {
                messagingTemplate.convertAndSend(destination, notification);
                log.debug("Sent {} to {}: requestId={}", notification.getType(), destination, notification.getRequestId());
            } catch (Exception e) {
                log.warn("Gửi WebSocket thất bại ({} -> {}): {}", notification.getType(), destination, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private MatchingNotification.MatchingNotificationBuilder base(String type, Long requestId, String message) {
        return MatchingNotification.builder()
                .type(type)
                .requestId(requestId)
                .message(message)
                .timestamp(System.currentTimeMillis());
    }


    public void sendMatchingRequest(Long workerUserId, Long requestId, Long matchingLogId) {
        push("/topic/user/" + workerUserId + "/matching",
                base("MATCHING_REQUEST", requestId, "Bạn có yêu cầu sửa chữa mới")
                        .matchingLogId(matchingLogId)
                        .build());
    }

    public void sendMatchingClosed(Long workerUserId, Long requestId) {
        push("/topic/user/" + workerUserId + "/matching",
                base("MATCHING_CLOSED", requestId, "Yêu cầu không còn khả dụng").build());
    }

    public void sendRequestCancelled(Long workerUserId, Long requestId) {
        push("/topic/user/" + workerUserId + "/matching",
                base("REQUEST_CANCELLED", requestId, "Khách đã hủy yêu cầu").build());
    }


    public void sendWorkerApproved(Long workerUserId) {
        push("/topic/user/" + workerUserId + "/account",
                base("WORKER_APPROVED", null, "Hồ sơ của bạn đã được duyệt. Bạn có thể bắt đầu nhận việc.").build());
    }

    public void sendWorkerRejected(Long workerUserId, String reason) {
        push("/topic/user/" + workerUserId + "/account",
                base("WORKER_PROFILE_REJECTED", null, "Hồ sơ của bạn chưa được duyệt. Lý do: " + reason).build());
    }

    public void sendWorkerRevoked(Long workerUserId, String reason) {
        push("/topic/user/" + workerUserId + "/account",
                base("WORKER_PROFILE_REVOKED", null, "Quyền nhận việc của bạn đã bị thu hồi. Lý do: " + reason).build());
    }


    public void sendWorkerAccepted(Long customerUserId, Long requestId, String workerName) {
        push("/topic/user/" + customerUserId + "/requests",
                base("WORKER_ACCEPTED", requestId, "Thợ " + workerName + " đã chấp nhận yêu cầu của bạn").build());
    }

    public void sendWorkerRejected(Long customerUserId, Long requestId, String workerName) {
        push("/topic/user/" + customerUserId + "/requests",
                base("WORKER_REJECTED", requestId, "Thợ " + workerName + " đã từ chối yêu cầu của bạn").build());
    }

    public void sendNoWorkerFound(Long customerUserId, Long requestId) {
        push("/topic/user/" + customerUserId + "/requests",
                base("NO_WORKER_FOUND", requestId, "Không tìm thấy thợ phù hợp cho yêu cầu của bạn").build());
    }

    public void sendNewQuotation(Long customerUserId, Long requestId, String workerName) {
        push("/topic/user/" + customerUserId + "/requests",
                base("NEW_QUOTATION", requestId, "Thợ " + workerName + " đã gửi báo giá cho bạn").build());
    }

    public void sendQuotationResponded(Long workerUserId, Long requestId, boolean accepted) {
        push("/topic/user/" + workerUserId + "/matching",
                base(accepted ? "QUOTATION_ACCEPTED" : "QUOTATION_REJECTED", requestId,
                        accepted ? "Khách đã chấp nhận báo giá" : "Khách đã từ chối báo giá").build());
    }

    public void sendReviewReceived(Long workerUserId, Long requestId, int rating) {
        push("/topic/user/" + workerUserId + "/matching",
                base("REVIEW_RECEIVED", requestId, "Khách đã đánh giá " + rating + " sao cho đơn của bạn").build());
    }

    public void sendWorkerOnTheWay(Long customerUserId, Long requestId) {
        push("/topic/user/" + customerUserId + "/requests",
                base("WORKER_ON_THE_WAY", requestId, "Thợ đang trên đường đến chỗ bạn").build());
    }

    public void sendWorkerArrived(Long customerUserId, Long requestId) {
        push("/topic/user/" + customerUserId + "/requests",
                base("WORKER_ARRIVED", requestId, "Thợ đã đến nơi").build());
    }

    public void sendJobCompleted(Long customerUserId, Long requestId) {
        push("/topic/user/" + customerUserId + "/requests",
                base("JOB_COMPLETED", requestId, "Yêu cầu sửa chữa đã hoàn thành").build());
    }

    public void sendWorkerLocation(Long customerUserId, Long requestId, double lat, double lng) {
        push("/topic/user/" + customerUserId + "/tracking",
                base("WORKER_LOCATION", requestId, "Vị trí thợ").lat(lat).lng(lng).build());
    }


    public void sendComplaintCreated(Long adminUserId, Long requestId, String requestCode) {
        push("/topic/user/" + adminUserId + "/complaints",
                base("COMPLAINT_CREATED", requestId, "Có khiếu nại mới cho đơn " + requestCode).build());
    }

    public void sendComplaintProcessing(Long complainantUserId, Long requestId) {
        push("/topic/user/" + complainantUserId + "/complaints",
                base("COMPLAINT_PROCESSING", requestId, "Khiếu nại của bạn đang được xử lý").build());
    }


    public void sendComplaintClosed(Long userId, Long requestId, boolean resolved, boolean forComplainant) {
        String type = resolved ? "COMPLAINT_RESOLVED" : "COMPLAINT_REJECTED";
        String message = forComplainant
                ? (resolved ? "Khiếu nại của bạn đã được giải quyết" : "Khiếu nại của bạn đã bị từ chối")
                : "Khiếu nại liên quan đến đơn của bạn đã được xử lý";
        push("/topic/user/" + userId + "/complaints", base(type, requestId, message).build());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MatchingNotification {
        private Double lat;
        private Double lng;
        private String type;
        private Long requestId;
        private Long matchingLogId;
        private String message;
        private Long timestamp;
    }
}
