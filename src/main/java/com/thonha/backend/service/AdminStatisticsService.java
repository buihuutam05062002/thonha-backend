package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.response.AdminStatisticsResponse;
import com.thonha.backend.dto.response.CommissionSummaryResponse;
import com.thonha.backend.dto.worker.WorkerIncomeResponse;
import com.thonha.backend.entity.Role;
import com.thonha.backend.enums.ApprovalStatus;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.enums.TransactionStatus;
import com.thonha.backend.enums.UserStatus;
import com.thonha.backend.repository.PaymentTransactionRepository;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.repository.ReviewRepository;
import com.thonha.backend.repository.UserRepository;
import com.thonha.backend.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 366;
    private static final int TOP_LIMIT = 5;

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentTransactionService paymentTransactionService;
    private final ComplaintService complaintService;

    @Transactional(readOnly = true)
    public AdminStatisticsResponse statistics(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate begin = from != null ? from : end.minusDays(DEFAULT_DAYS - 1L);
        if (end.isBefore(begin)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        if (end.toEpochDay() - begin.toEpochDay() + 1 > MAX_DAYS) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Khoảng ngày tối đa là " + MAX_DAYS + " ngày");
        }
        LocalDateTime start = begin.atStartOfDay();
        LocalDateTime endExclusive = end.plusDays(1).atStartOfDay();

        CommissionSummaryResponse revenue = paymentTransactionService.summarize(begin, end);
        return new AdminStatisticsResponse(
                begin, end,
                users(start, endExclusive),
                requests(start, endExclusive),
                revenue,
                complaintService.stats(),
                reviews(start, endExclusive),
                daily(begin, end, start, endExclusive),
                topCategories(start, endExclusive),
                topWorkers(start, endExclusive));
    }

    private AdminStatisticsResponse.Users users(LocalDateTime start, LocalDateTime endExclusive) {
        long approved = 0;
        long pending = 0;
        for (Object[] row : workerProfileRepository.countByApprovalStatus()) {
            long count = ((Number) row[1]).longValue();
            if (row[0] == ApprovalStatus.APPROVED) approved = count;
            else if (row[0] == ApprovalStatus.PENDING) pending = count;
        }
        return new AdminStatisticsResponse.Users(
                userRepository.countByRole(Role.CUSTOMER, UserStatus.DELETED),
                approved,
                pending,
                userRepository.countByRoleCreatedBetween(Role.CUSTOMER, UserStatus.DELETED, start, endExclusive),
                workerProfileRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, endExclusive));
    }

    private AdminStatisticsResponse.Requests requests(LocalDateTime start, LocalDateTime endExclusive) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (RepairStatus s : RepairStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        long total = 0;
        for (Object[] row : repairRequestRepository.countByStatusBetween(start, endExclusive)) {
            long count = ((Number) row[1]).longValue();
            byStatus.put(((RepairStatus) row[0]).name(), count);
            total += count;
        }
        long completed = byStatus.get(RepairStatus.COMPLETED.name());
        long cancelled = byStatus.get(RepairStatus.CANCELLED.name());
        double rate = total == 0 ? 0
                : BigDecimal.valueOf(completed * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
        return new AdminStatisticsResponse.Requests(total, completed, cancelled, rate, byStatus);
    }

    private AdminStatisticsResponse.Reviews reviews(LocalDateTime start, LocalDateTime endExclusive) {
        List<Object[]> rows = reviewRepository.summarizeBetween(start, endExclusive);
        if (rows.isEmpty()) return new AdminStatisticsResponse.Reviews(0, 0);
        Object[] row = rows.get(0);
        double avg = row[0] == null ? 0
                : BigDecimal.valueOf(((Number) row[0]).doubleValue()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        long count = row[1] == null ? 0 : ((Number) row[1]).longValue();
        return new AdminStatisticsResponse.Reviews(avg, count);
    }

    /** Mỗi ngày trong khoảng: số yêu cầu mới + doanh thu/hoa hồng đã chốt (ngày trống bằng 0). */
    private List<AdminStatisticsResponse.Daily> daily(LocalDate begin, LocalDate end,
                                                      LocalDateTime start, LocalDateTime endExclusive) {
        int days = (int) (end.toEpochDay() - begin.toEpochDay()) + 1;
        long[] requests = new long[days];
        for (LocalDateTime at : repairRequestRepository.findCreatedAtBetween(start, endExclusive)) {
            long idx = at.toLocalDate().toEpochDay() - begin.toEpochDay();
            if (idx >= 0 && idx < days) requests[(int) idx]++;
        }
        List<IncomeDailyAggregator.Entry> entries = paymentTransactionRepository
                .findEntries(TransactionStatus.SUCCESS, start, endExclusive).stream()
                .map(r -> new IncomeDailyAggregator.Entry(
                        r.getCreatedAt(), r.getAmount(), r.getCommission(), r.getWorkerAmount()))
                .toList();
        List<WorkerIncomeResponse.Daily> income = IncomeDailyAggregator.aggregate(begin, end, entries);

        List<AdminStatisticsResponse.Daily> result = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            WorkerIncomeResponse.Daily d = income.get(i);
            result.add(new AdminStatisticsResponse.Daily(
                    begin.plusDays(i), requests[i], d.orders(), d.grossAmount(), d.commissionAmount()));
        }
        return result;
    }

    private List<AdminStatisticsResponse.CategoryCount> topCategories(LocalDateTime start, LocalDateTime endExclusive) {
        return repairRequestRepository.topCategoriesBetween(start, endExclusive, PageRequest.of(0, TOP_LIMIT)).stream()
                .map(r -> new AdminStatisticsResponse.CategoryCount((String) r[0], ((Number) r[1]).longValue()))
                .toList();
    }

    private List<AdminStatisticsResponse.TopWorker> topWorkers(LocalDateTime start, LocalDateTime endExclusive) {
        return paymentTransactionRepository
                .topWorkers(TransactionStatus.SUCCESS, start, endExclusive, PageRequest.of(0, TOP_LIMIT)).stream()
                .map(r -> new AdminStatisticsResponse.TopWorker(
                        ((Number) r[0]).longValue(), (String) r[1], ((Number) r[2]).longValue(),
                        nz((BigDecimal) r[3]), nz((BigDecimal) r[4])))
                .toList();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
