package com.thonha.backend.controller;

import com.thonha.backend.dto.request.SepayRequest;
import com.thonha.backend.service.WalletTopUpService;
import com.thonha.backend.service.SePayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook SePay (báo thợ đã chuyển khoản nạp ví). Không dùng JWT: xác thực bằng header "Authorization: Apikey <SEPAY_API_KEY>".
 * SePay yêu cầu phản hồi {"success": true} (HTTP 200/201), nên không bọc ApiResponse.
 */
@RestController
@RequestMapping("/api/v1/payments/sepay")
@Slf4j
public class SepayWebhookController {

    private final SePayService sePayService;
    private final WalletTopUpService topUpService;

    public SepayWebhookController(SePayService sePayService, WalletTopUpService topUpService) {
        this.sePayService = sePayService;
        this.topUpService = topUpService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> webhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SepayRequest payload) {
        if (!sePayService.isValidApiKey(authorization)) {
            log.warn("Sepay webhook bị từ chối: thiếu hoặc sai API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }
        String outcome = topUpService.handleSepayWebhook(payload);
        log.info("Sepay webhook id={} -> {}", payload.getId(), outcome);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
