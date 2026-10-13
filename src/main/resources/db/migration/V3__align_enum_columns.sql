-- Đồng bộ các cột ENUM của V1 với enum Java (@Enumerated(EnumType.STRING)).
-- V1 dùng giá trị chữ thường / tên cũ (vd 'pending', 'assigned', 'timeout'), Java ghi PENDING_MATCH, MATCHED, EXPIRED...
-- nên INSERT/UPDATE bị lỗi "Data truncated". Chuyển sang VARCHAR rồi quy đổi dữ liệu cũ (nếu có).
-- Phạm vi: các bảng mà module Khách hàng dùng (repair_request, matching_log, users, service_category).

-- repair_request
ALTER TABLE repair_request
    MODIFY priority_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    MODIFY status         VARCHAR(30) NOT NULL DEFAULT 'PENDING_MATCH';

UPDATE repair_request SET priority_level = CASE LOWER(priority_level)
    WHEN 'normal' THEN 'MEDIUM'
    WHEN 'urgent' THEN 'URGENT'
    ELSE UPPER(priority_level) END;

UPDATE repair_request SET status = CASE LOWER(status)
    WHEN 'pending'     THEN 'PENDING_MATCH'
    WHEN 'assigned'    THEN 'MATCHED'
    WHEN 'matching'    THEN 'MATCHING'
    WHEN 'in_progress' THEN 'IN_PROGRESS'
    WHEN 'completed'   THEN 'COMPLETED'
    WHEN 'cancelled'   THEN 'CANCELLED'
    ELSE UPPER(status) END;

-- matching_log: thêm EXPIRED, CANCELLED_MIDWAY
ALTER TABLE matching_log
    MODIFY result VARCHAR(30) NOT NULL DEFAULT 'PENDING';

UPDATE matching_log SET result = CASE LOWER(result)
    WHEN 'timeout' THEN 'EXPIRED'
    ELSE UPPER(result) END;

-- users: thêm LOCKED, DELETED (V1 chỉ có 'banned')
ALTER TABLE users
    MODIFY status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE users SET status = CASE LOWER(status)
    WHEN 'banned' THEN 'LOCKED'
    ELSE UPPER(status) END;

-- service_category
ALTER TABLE service_category
    MODIFY status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE service_category SET status = UPPER(status);
