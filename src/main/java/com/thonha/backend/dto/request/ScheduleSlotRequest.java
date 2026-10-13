package com.thonha.backend.dto.request;

import com.thonha.backend.enums.ScheduleStatus;
import jakarta.validation.constraints.NotNull;
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
public class ScheduleSlotRequest {

    @NotNull(message = "Vui lòng chọn ngày")
    LocalDate workDate;

    @NotNull(message = "Vui lòng chọn giờ bắt đầu")
    LocalTime startTime;

    @NotNull(message = "Vui lòng chọn giờ kết thúc")
    LocalTime endTime;

    ScheduleStatus status;
}
