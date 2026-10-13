package com.thonha.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerLocationUpdate {
    private Long requestId;
    private Double workerLat;
    private Double workerLng;
    private long timestamp;
}
