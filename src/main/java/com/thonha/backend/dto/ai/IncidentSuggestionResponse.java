package com.thonha.backend.dto.ai;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Kết quả AI phân loại sự cố. categoryId = null nghĩa là AI không đủ chắc chắn,
 * FE nên để khách tự chọn danh mục.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class IncidentSuggestionResponse {
    Long categoryId;
    String categoryName;
    /** 0.0 - 1.0 */
    Double confidence;
    /** Tóm tắt sự cố ngắn gọn, khách có thể dùng làm mô tả */
    String summary;
    /** Lý do AI chọn danh mục này */
    String reason;
}
