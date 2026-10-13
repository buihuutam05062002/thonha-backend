package com.thonha.backend.service;

import com.thonha.backend.dto.worker.WorkerIncomeResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncomeDailyAggregatorTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static IncomeDailyAggregator.Entry entry(String dateTime, String gross, String commission, String net) {
        return new IncomeDailyAggregator.Entry(LocalDateTime.parse(dateTime), d(gross), d(commission), d(net));
    }

    @Test
    void coDuMoiNgayTrongKhoangKeCaNgayKhongCoDon() {
        var from = LocalDate.of(2026, 10, 5);
        var to = LocalDate.of(2026, 10, 11);
        var result = IncomeDailyAggregator.aggregate(from, to, List.of());
        assertEquals(7, result.size());
        assertEquals(from, result.get(0).date());
        assertEquals(to, result.get(6).date());
        result.forEach(day -> {
            assertEquals(0, day.orders());
            assertEquals(0, BigDecimal.ZERO.compareTo(day.workerAmount()));
        });
    }

    @Test
    void congDonCuaCungMotNgayVaTachRiengCacNgay() {
        var from = LocalDate.of(2026, 10, 5);
        var to = LocalDate.of(2026, 10, 7);
        var result = IncomeDailyAggregator.aggregate(from, to, List.of(
                entry("2026-10-05T00:00:00", "350000", "35000", "315000"),
                entry("2026-10-05T23:59:59", "100000", "10000", "90000"),
                entry("2026-10-07T12:00:00", "200000", "20000", "180000")));
        WorkerIncomeResponse.Daily first = result.get(0);
        assertEquals(2, first.orders());
        assertEquals(0, d("450000").compareTo(first.grossAmount()));
        assertEquals(0, d("45000").compareTo(first.commissionAmount()));
        assertEquals(0, d("405000").compareTo(first.workerAmount()));
        assertEquals(0, result.get(1).orders());
        assertEquals(1, result.get(2).orders());
        assertEquals(0, d("180000").compareTo(result.get(2).workerAmount()));
    }

    @Test
    void boQuaGiaoDichNgoaiKhoangVaCoiSoTienNullLaKhong() {
        var from = LocalDate.of(2026, 10, 5);
        var to = LocalDate.of(2026, 10, 5);
        var result = IncomeDailyAggregator.aggregate(from, to, List.of(
                entry("2026-10-04T23:59:59", "999", "99", "900"),
                entry("2026-10-06T00:00:00", "999", "99", "900"),
                new IncomeDailyAggregator.Entry(LocalDateTime.parse("2026-10-05T08:00:00"), d("50000"), null, null)));
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).orders());
        assertEquals(0, d("50000").compareTo(result.get(0).grossAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).commissionAmount()));
    }
}
