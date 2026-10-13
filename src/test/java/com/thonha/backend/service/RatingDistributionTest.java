package com.thonha.backend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RatingDistributionTest {

    @Test
    void chuaCoDanhGiaThiDiemTrungBinhLaNullVaMoiMucSaoBangKhong() {
        var s = RatingDistribution.summarize(Map.of());
        assertNull(s.averageRating());
        assertEquals(0, s.totalReviews());
        assertEquals(List.of(5, 4, 3, 2, 1), List.copyOf(s.distribution().keySet()));
        s.distribution().values().forEach(v -> assertEquals(0L, v));
    }

    @Test
    void tinhDiemTrungBinhLamTronMotChuSo() {
        var s = RatingDistribution.summarize(Map.of(5, 5L, 4, 2L, 1, 1L));
        assertEquals(new BigDecimal("4.3"), s.averageRating());
        assertEquals(8, s.totalReviews());
        assertEquals(5L, s.distribution().get(5));
        assertEquals(0L, s.distribution().get(3));
    }

    @Test
    void boQuaMucSaoNgoaiKhoang1Den5() {
        var s = RatingDistribution.summarize(Map.of(3, 2L, 0, 7L, 6, 9L));
        assertEquals(2, s.totalReviews());
        assertEquals(new BigDecimal("3.0"), s.averageRating());
    }
}
