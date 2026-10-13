package com.thonha.backend.controller;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.dto.tracking.LocationMessage;
import com.thonha.backend.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * Thợ gửi vị trí: SEND /app/requests/{id}/location.
 * Mọi kiểm tra (đúng thợ phụ trách, đúng trạng thái, tọa độ hợp lệ, chống gửi dày) nằm trong
 * {@link TrackingService#handleLocation}. Lỗi trả riêng cho người gửi ở /user/queue/errors
 * theo cùng định dạng {code, message} với ChatWsController.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class TrackingWsController {

    private final TrackingService trackingService;

    @MessageMapping("/requests/{id}/location")
    public void location(@DestinationVariable("id") Long id,
                         @Payload(required = false) LocationMessage msg,
                         Principal principal) {
        trackingService.handleLocation(id, Long.valueOf(principal.getName()), msg);
    }

    @MessageExceptionHandler(ApiException.class)
    @SendToUser("/queue/errors")
    public Map<String, String> onApiError(ApiException e) {
        return Map.of("code", e.getCode(), "message", e.getMessage());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, String> onError(Exception e) {
        log.warn("Lỗi khi xử lý vị trí qua WebSocket", e);
        return Map.of("code", "INTERNAL_ERROR", "message", "Không gửi được vị trí, vui lòng thử lại");
    }
}
