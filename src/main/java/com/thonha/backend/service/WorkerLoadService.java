package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.repository.WorkerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class WorkerLoadService {

    public static final int MAX_ACTIVE_JOBS = 3;

    private final WorkerProfileRepository workerProfileRepository;
    private final WalletService walletService;

    public WorkerLoadService(WorkerProfileRepository workerProfileRepository, WalletService walletService) {
        this.workerProfileRepository = workerProfileRepository;
        this.walletService = walletService;
    }

    @Transactional
    public void assignJob(Long workerProfileId) {
        walletService.assertCanTakeJobs(workerProfileId); // ví thấp hơn mức tối thiểu thì không nhận thêm đơn
        if (workerProfileRepository.tryIncrementOngoing(workerProfileId, MAX_ACTIVE_JOBS) == 0) {
            throw new ApiException(ErrorCode.WORKER_HAS_ONGOING_JOBS,
                    "Bạn đang làm tối đa " + MAX_ACTIVE_JOBS + " đơn cùng lúc, hãy hoàn thành bớt trước khi nhận thêm");
        }
        workerProfileRepository.markBusyIfFull(workerProfileId, MAX_ACTIVE_JOBS);
    }

    @Transactional
    public void releaseJob(Long workerProfileId) {
        if (workerProfileRepository.decrementOngoing(workerProfileId) > 0) {
            workerProfileRepository.markReadyIfFree(workerProfileId, MAX_ACTIVE_JOBS);
        }
    }
}
