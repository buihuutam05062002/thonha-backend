package com.thonha.backend.dto.response;

import com.thonha.backend.entity.WorkerSchedule;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class ScheduleSlotResponse {
     Long id;
     LocalDate workDate;
     LocalTime startTime;
     LocalTime endTime;
     String status;
     String statusDescription;

    public static ScheduleSlotResponse from(WorkerSchedule s) {
        return ScheduleSlotResponse.builder()
                .id(s.getId())
                .workDate(s.getWorkDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .status(s.getStatus().name())
                .statusDescription(s.getStatus().getDescription())
                .build();
    }
}
