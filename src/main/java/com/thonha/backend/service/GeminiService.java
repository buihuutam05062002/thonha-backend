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
import io.netty.channel.ChannelOption;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
public class GeminiService {

    public record InlineImage(String mimeType, byte[] data) {}

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final List<String> models;
    private final int timeoutSeconds;
    private final long totalBudgetNanos;

    public GeminiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                         @Value("${gemini.api-key:}") String apiKey,
                         @Value("${gemini.model:gemini-flash-latest}") String model,
                         @Value("${gemini.fallback-models:}") String fallbackModels,
                         @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
                         @Value("${gemini.timeout-seconds:15}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        List<String> list = new ArrayList<>();
        list.add(model.trim());
        for (String m : fallbackModels.split(",")) {
            if (!m.isBlank() && !list.contains(m.trim())) list.add(m.trim());
        }
        this.models = list;
        this.timeoutSeconds = Math.max(5, timeoutSeconds);
        this.totalBudgetNanos = Duration.ofSeconds(this.timeoutSeconds * 2L + 10).toNanos();

        HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    public JsonNode generateJson(String prompt, List<InlineImage> images, ObjectNode responseSchema) {
        if (apiKey.isBlank()) {
            throw new ApiException(ErrorCode.CONFIGURATION_ERROR,
                    "Chưa cấu hình GEMINI_API_KEY ở backend nên chưa dùng được AI");
        }

        ApiException last = null;
        long deadline = System.nanoTime() + totalBudgetNanos;
        outer:
        for (String model : models) {

            for (int attempt = 1; attempt <= 2; attempt++) {
                long started = System.nanoTime();
                try {
                    JsonNode result = callModel(model, prompt, images, responseSchema);
                    log.info("Gemini '{}' phản hồi sau {} ms (lần thử {})", model, (System.nanoTime() - started) / 1_000_000, attempt);
                    return result;
                } catch (KeyProblem e) {

                    throw new ApiException(ErrorCode.CONFIGURATION_ERROR, e.getMessage());
                } catch (TransientApiException e) {
                    last = e;
                    log.warn("Gemini model '{}' lỗi tạm thời sau {} ms (lần thử {}/2): {}", model,
                            (System.nanoTime() - started) / 1_000_000, attempt, e.getMessage());
                    if (System.nanoTime() >= deadline) break outer;
                    if (attempt == 1) pause(600);
                } catch (ApiException e) {
                    last = e;
                    log.warn("Gemini model '{}' không dùng được ({}), thử model tiếp theo nếu có", model, e.getMessage());
                    break;
                }
            }
            if (System.nanoTime() >= deadline) break;
        }
        throw last != null ? last : new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI không trả về kết quả");
    }

    private JsonNode callModel(String model, String prompt, List<InlineImage> images, ObjectNode responseSchema) {
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

        if (model.contains("2.5-flash")) {
            gen.putObject("thinkingConfig").put("thinkingBudget", 0);
        }

        String res;
        try {
            res = webClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
        } catch (WebClientResponseException e) {
            throw translate(model, e);
        } catch (Exception e) {
            log.warn("Gemini '{}' lỗi kết nối/timeout: {}", model, e.toString());
            throw new TransientApiException(
                    "Không kết nối được tới dịch vụ AI (mạng hoặc quá thời gian chờ), bạn có thể chọn danh mục thủ công");
        }

        try {
            JsonNode root = objectMapper.readTree(res);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            if (text.isBlank()) {
                String block = root.path("promptFeedback").path("blockReason").asText("");
                log.warn("Gemini '{}' trả về rỗng. blockReason='{}', finishReason='{}'", model, block,
                        root.path("candidates").path(0).path("finishReason").asText(""));
                throw new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI không trả về kết quả");
            }
            return objectMapper.readTree(text);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini '{}' trả dữ liệu không đọc được: {}", model, e.toString());
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "AI trả về dữ liệu không hợp lệ");
        }
    }

    private ApiException translate(String model, WebClientResponseException e) {
        int status = e.getStatusCode().value();
        String googleMsg = extractMessage(e.getResponseBodyAsString());
        log.warn("Gemini '{}' HTTP {}: {}", model, status, googleMsg);

        String lower = googleMsg.toLowerCase();
        if (status == 401 || status == 403 || lower.contains("api key not valid") || lower.contains("api_key_invalid")
                || lower.contains("api key expired") || lower.contains("permission denied")) {
            throw new KeyProblem("GEMINI_API_KEY không hợp lệ hoặc chưa được bật Gemini API: " + googleMsg);
        }
        if (status == 429) {
            return new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "AI đang hết hạn mức hoặc quá tải, bạn thử lại sau ít phút hoặc chọn danh mục thủ công");
        }
        if (status == 404) {
            return new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Model AI '" + model + "' không tồn tại, hãy kiểm tra GEMINI_MODEL");
        }
        if (status >= 500) {
            return new TransientApiException(
                    "Dịch vụ AI đang quá tải, bạn thử lại sau hoặc chọn danh mục thủ công");
        }
        if (lower.contains("location is not supported")) {
            return new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Gemini API không hỗ trợ khu vực của máy chủ backend: " + googleMsg);
        }

        return new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Không phân tích được bằng AI lúc này (Gemini HTTP " + status + ": " + googleMsg
                        + "), bạn có thể chọn danh mục thủ công");
    }

    private String extractMessage(String body) {
        try {
            String m = objectMapper.readTree(body).path("error").path("message").asText("");
            if (!m.isBlank()) return m;
        } catch (Exception ignored) {

        }
        return body == null ? "" : body.length() > 300 ? body.substring(0, 300) : body;
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class TransientApiException extends ApiException {
        TransientApiException(String message) {
            super(ErrorCode.EXTERNAL_SERVICE_ERROR, message);
        }
    }

    private static class KeyProblem extends RuntimeException {
        KeyProblem(String message) {
            super(message);
        }
    }
}