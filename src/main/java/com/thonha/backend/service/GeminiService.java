package com.thonha.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * Client mỏng gọi Gemini REST API (generateContent). Chỉ lo việc gửi prompt + ảnh
 * và trả về JSON do model sinh ra; logic nghiệp vụ nằm ở IncidentClassifierService.
 */
@Service
@Slf4j
public class GeminiService {

    /** Ảnh đính kèm đã đọc vào bộ nhớ */
    public record InlineImage(String mimeType, byte[] data) {}

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                         @Value("${gemini.api-key:}") String apiKey,
                         @Value("${gemini.model}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    /**
     * Gửi prompt + ảnh, ép model trả về JSON theo schema.
     *
     * @param responseSchema schema kiểu OpenAPI mà Gemini hỗ trợ (type/properties/required)
     * @return JsonNode đã parse từ text model trả về
     */
    public JsonNode generateJson(String prompt, List<InlineImage> images, ObjectNode responseSchema) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(ErrorCode.CONFIGURATION_ERROR, "Chưa cấu hình GEMINI_API_KEY");
        }

        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode parts = body.putArray("contents").addObject().putArray("parts");
        parts.addObject().put("text", prompt);
        for (InlineImage img : images) {
            ObjectNode inline = parts.addObject().putObject("inline_data");
            inline.put("mime_type", img.mimeType());
            inline.put("data", Base64.getEncoder().encodeToString(img.data()));
        }

        ObjectNode gen = body.putObject("generationConfig");
        gen.put("temperature", 0.1);
        gen.put("responseMimeType", "application/json");
        gen.set("responseSchema", responseSchema);
        // Phân loại đơn giản, không cần "suy nghĩ" dài: giảm độ trễ và chi phí
        gen.putObject("thinkingConfig").put("thinkingBudget", 0);

        try {
            String res = webClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(40))
                    .block();

            String text = objectMapper.readTree(res)
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            if (text.isBlank()) {
                throw new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI không trả về kết quả");
            }
            return objectMapper.readTree(text);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini call failed: {}", e.getMessage());
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Không phân tích được bằng AI lúc này, bạn có thể chọn danh mục thủ công");
        }
    }
}
