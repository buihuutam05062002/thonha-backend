package com.thonha.backend.config;

import com.thonha.backend.repository.WalletRepository;
import com.thonha.backend.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Khi khởi động, tạo ví cho các thợ đã được duyệt từ trước khi có tính năng ví.
 * Idempotent: chạy lại nhiều lần hay nhiều instance cùng chạy đều an toàn.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletBackfillRunner implements ApplicationRunner {

    private final WalletRepository walletRepository;
    private final WalletService walletService;

    @Override
    public void run(ApplicationArguments args) {
        List<Long> missing = walletRepository.findApprovedWorkerIdsWithoutWallet();
        int created = 0;
        for (Long workerProfileId : missing) {
            try {
                walletService.ensureWallet(workerProfileId); // mỗi thợ một transaction riêng
                created++;
            } catch (RuntimeException e) {
                log.error("Không tạo được ví cho thợ {}", workerProfileId, e);
            }
        }
        if (created > 0) {
            log.info("Đã tạo ví cho {} thợ đã duyệt trước đó", created);
        }
    }
}
