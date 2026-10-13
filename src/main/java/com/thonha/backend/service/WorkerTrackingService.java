package com.thonha.backend.service;

import com.thonha.backend.dto.request.WorkerLocationMessage;
import com.thonha.backend.dto.response.WorkerLocationUpdate;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.repository.RepairRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerTrackingService {
    private final RepairRequestRepository repairRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Transactional(readOnly = true)
    public void forwardLocation(Long workerUserId, WorkerLocationMessage message){
        if (message == null||message.getRequestId() ==null || !isValidLat(message.getWorkerLat()) || !isValidLng(message.getWorkerLng())){
            return;
        }

        RepairRequest request = repairRequestRepository.findById(message.getRequestId()).orElse(null);
        if (request ==null || request.getWorker() == null || !request.getWorker().getUser().getId().equals(workerUserId) || request.getStatus() != RepairStatus.ON_THE_WAY){
            log.debug("Ignored location of user {} for request {}", workerUserId, message.getRequestId());
            return;
        }
        messagingTemplate.convertAndSend("/topic/user/" + request.getCustomer().getId() + "/worker-location",
                new WorkerLocationUpdate(request.getId(), message.getWorkerLat(), message.getWorkerLng(), System.currentTimeMillis()));
    }
    
    private static boolean isValidLat(Double lat){
        return lat != null && lat >= -90 && lat <= 90;
    }
    
    private static boolean isValidLng(Double lng){
        return lng != null && lng >= -180 && lng <= 180;
    }
}
