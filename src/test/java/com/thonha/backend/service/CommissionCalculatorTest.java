package com.thonha.backend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommissionCalculatorTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    void tinhHoaHongMuoiPhanTram() {
        var r = CommissionCalculator.calculate(d("350000"), d("10"));
        assertEquals(d("35000.00"), r.commissionAmount());
        assertEquals(d("315000.00"), r.workerAmount());
    }

    @Test
    void lamTronHalfUpVeDong() {
        assertEquals(d("10001.00"), CommissionCalculator.calculate(d("100005"), d("10")).commissionAmount());
        assertEquals(d("10000.00"), CommissionCalculator.calculate(d("100004"), d("10")).commissionAmount());
        assertEquals(d("12500.00"), CommissionCalculator.calculate(d("99999"), d("12.5")).commissionAmount());
    }

    @Test
    void hoaHongCongPhanThoLuonBangGia() {
        String[] prices = {"1", "7", "33333", "100005", "999999", "1234567.89"};
        String[] rates = {"0", "0.01", "3.33", "10", "12.5", "33.33", "99.99", "100"};
        for (String p : prices) {
            for (String rt : rates) {
                var r = CommissionCalculator.calculate(d(p), d(rt));
                assertEquals(d(p).setScale(2), r.commissionAmount().add(r.workerAmount()), p + " @ " + rt);
                assertEquals(true, r.workerAmount().signum() >= 0, "phần thợ không âm: " + p + " @ " + rt);
            }
        }
    }

    @Test
    void tyLeBienKhong() {
        var zero = CommissionCalculator.calculate(d("500000"), d("0"));
        assertEquals(d("0.00"), zero.commissionAmount());
        assertEquals(d("500000.00"), zero.workerAmount());

        var full = CommissionCalculator.calculate(d("500000"), d("100"));
        assertEquals(d("500000.00"), full.commissionAmount());
        assertEquals(d("0.00"), full.workerAmount());
    }

    @Test
    void tuChoiDauVaoKhongHopLe() {
        assertThrows(IllegalArgumentException.class, () -> CommissionCalculator.calculate(null, d("10")));
        assertThrows(IllegalArgumentException.class, () -> CommissionCalculator.calculate(d("0"), d("10")));
        assertThrows(IllegalArgumentException.class, () -> CommissionCalculator.calculate(d("-1"), d("10")));
        assertThrows(IllegalArgumentException.class, () -> CommissionCalculator.calculate(d("1000"), null));
        assertThrows(IllegalArgumentException.class, () -> CommissionCalculator.calculate(d("1000"), d("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> CommissionCalculator.calculate(d("1000"), d("100.01")));
    }
}
