package com.thonha.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarrantyNotifier {
    private final SimpMessagingTemplate messagingTemplate;

    public record WarrantyEvent(String type, Long claimId, Long requestId, String message) {
    }

    public void notifyUser(Long userId, String type, Long claimId, Long requestId, String message) {
        WarrantyEvent payload = new WarrantyEvent(type, claimId, requestId, message);
        Runnable send = () -> {
            try {
                messagingTemplate.convertAndSend("/topic/user/" + userId + "/warranty", payload);
            } catch (Exception e) {
                log.warn("Warranty notification failed for user {}: {}", userId, e.getMessage());
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
}
