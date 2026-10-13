package com.thonha.backend.service;

import com.thonha.backend.dto.worker.WorkerIncomeResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class IncomeDailyAggregator {

    private IncomeDailyAggregator() {
    }

    record Entry(LocalDateTime at, BigDecimal gross, BigDecimal commission, BigDecimal net) {
    }

    static List<WorkerIncomeResponse.Daily> aggregate(LocalDate from, LocalDate to, List<Entry> entries) {
        int days = (int) (to.toEpochDay() - from.toEpochDay()) + 1;
        BigDecimal[] gross = new BigDecimal[days];
        BigDecimal[] commission = new BigDecimal[days];
        BigDecimal[] net = new BigDecimal[days];
        int[] orders = new int[days];
        for (int i = 0; i < days; i++) {
            gross[i] = BigDecimal.ZERO;
            commission[i] = BigDecimal.ZERO;
            net[i] = BigDecimal.ZERO;
        }
        for (Entry e : entries) {
            long idx = e.at().toLocalDate().toEpochDay() - from.toEpochDay();
            if (idx < 0 || idx >= days) continue; // ngoài khoảng: bỏ qua
            int i = (int) idx;
            gross[i] = gross[i].add(nz(e.gross()));
            commission[i] = commission[i].add(nz(e.commission()));
            net[i] = net[i].add(nz(e.net()));
            orders[i]++;
        }
        List<WorkerIncomeResponse.Daily> result = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            result.add(new WorkerIncomeResponse.Daily(from.plusDays(i), gross[i], commission[i], net[i], orders[i]));
        }
        return result;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
