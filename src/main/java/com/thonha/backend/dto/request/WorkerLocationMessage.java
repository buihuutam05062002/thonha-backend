package com.thonha.backend.dto.request;

import lombok.Data;

@Data
public class WorkerLocationMessage {
    private Long requestId;
    private Double workerLat;
    private Double workerLng;
}
