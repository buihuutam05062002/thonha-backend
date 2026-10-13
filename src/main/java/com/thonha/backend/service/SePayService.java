package com.thonha.backend.service;

import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SePayService {
    @Value("${sepay.bank-account}")
    String bankAccount;

    @Value("${sepay.bank-name}")
    String bankName;

    @Value("${sepay.accountHolder}")
    String accountHolder;


    public String buildQrUrl(BigDecimal amount, String des) {
        return "https://qr.sepay.vn/img?"
                + "acc=" + URLEncoder.encode(bankAccount, StandardCharsets.UTF_8)
                + "&bank=" + URLEncoder.encode(bankName, StandardCharsets.UTF_8)
                + "&amount=" + amount
                + "&des=" + URLEncoder.encode(des, StandardCharsets.UTF_8)
                + "&template=compact"
                + "&showinfo=false"
                + "&holder=" + URLEncoder.encode(accountHolder, StandardCharsets.UTF_8);
    }

}
