package com.thonha.backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    SUCCESS(HttpStatus.OK, "SUCCESS", "Thành công"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu không hợp lệ"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Yêu cầu không hợp lệ"),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "MISSING_REQUIRED_FIELD", "Thiếu trường bắt buộc"),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_FORMAT", "Định dạng không hợp lệ"),
    DUPLICATE_ENTRY(HttpStatus.BAD_REQUEST, "DUPLICATE_ENTRY", "Dữ liệu đã tồn tại"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE", "Loại file không được hỗ trợ"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "Kích thước file quá lớn"),
    INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "INVALID_CREDENTIALS", "Thông tin đăng nhập không chính xác"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Chưa xác thực"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token không hợp lệ hoặc đã hết hạn"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Token đã hết hạn"),
    INVALID_CREDENTIALS_LOGIN(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS_LOGIN", "Email/SĐT hoặc mật khẩu không chính xác"),
    ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "ACCOUNT_LOCKED", "Tài khoản đã bị khóa"),
    ACCOUNT_INACTIVE(HttpStatus.UNAUTHORIZED, "ACCOUNT_INACTIVE", "Tài khoản chưa được kích hoạt"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "Refresh token đã hết hạn"),
    REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REVOKED", "Refresh token đã bị thu hồi"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "Không có quyền truy cập"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Bị từ chối truy cập"),
    ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "Cần quyền quản trị viên"),
    WORKER_REQUIRED(HttpStatus.FORBIDDEN, "WORKER_REQUIRED", "Cần quyền thợ"),
    CUSTOMER_REQUIRED(HttpStatus.FORBIDDEN, "CUSTOMER_REQUIRED", "Cần quyền khách hàng"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy tài nguyên"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng"),
    WORKER_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKER_NOT_FOUND", "Không tìm thấy hồ sơ thợ"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Không tìm thấy danh mục"),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Không tìm thấy yêu cầu sửa chữa"),
    COMPLAINT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPLAINT_NOT_FOUND", "Không tìm thấy khiếu nại"),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "REFRESH_TOKEN_NOT_FOUND", "Không tìm thấy refresh token"),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "Xung đột dữ liệu"),
    EMAIL_EXISTS(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email đã được sử dụng"),
    PHONE_EXISTS(HttpStatus.CONFLICT, "PHONE_EXISTS", "Số điện thoại đã được sử dụng"),
    USERNAME_EXISTS(HttpStatus.CONFLICT, "USERNAME_EXISTS", "Tên đăng nhập đã được sử dụng"),
    WORKER_PROFILE_EXISTS(HttpStatus.CONFLICT, "WORKER_PROFILE_EXISTS", "Hồ sơ thợ đã tồn tại"),
    ADDRESS_ALREADY_DEFAULT(HttpStatus.CONFLICT, "ADDRESS_ALREADY_DEFAULT", "Địa chỉ đã là mặc định"),
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, "UNPROCESSABLE_ENTITY", "Không thể xử lý yêu cầu"),
    WORKER_NOT_APPROVED(HttpStatus.UNPROCESSABLE_ENTITY, "WORKER_NOT_APPROVED", "Hồ sơ thợ chưa được duyệt"),
    WORKER_HAS_ONGOING_JOBS(HttpStatus.UNPROCESSABLE_ENTITY, "WORKER_HAS_ONGOING_JOBS", "Thợ đang có việc đang thực hiện"),
    REQUEST_NOT_PENDING(HttpStatus.UNPROCESSABLE_ENTITY, "REQUEST_NOT_PENDING", "Yêu cầu không ở trạng thái chờ duyệt"),
    INVALID_STATUS_TRANSITION(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_STATUS_TRANSITION", "Chuyển trạng thái không hợp lệ"),
    MISSING_REQUIRED_DOCUMENTS(HttpStatus.UNPROCESSABLE_ENTITY, "MISSING_REQUIRED_DOCUMENTS", "Thiếu tài liệu bắt buộc"),
    WORKER_HAS_ONGOING_JOB(HttpStatus.UNPROCESSABLE_ENTITY, "WORKER_HAS_ONGOING_JOB", "Thợ đang có công việc đang thực hiện"),
    WALLET_BALANCE_TOO_LOW(HttpStatus.UNPROCESSABLE_ENTITY, "WALLET_BALANCE_TOO_LOW", "Số dư ví thấp hơn mức tối thiểu để nhận đơn, vui lòng nạp tiền vào ví để tiếp tục nhận việc"),
    FINAL_PRICE_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "FINAL_PRICE_REQUIRED", "Vui lòng nhập giá thực tế (hoặc để khách chấp nhận báo giá) trước khi hoàn thành đơn"),
    WARRANTY_NOT_FOUND(HttpStatus.NOT_FOUND, "WARRANTY_NOT_FOUND", "Không tìm thấy phiếu bảo hành"),
    WARRANTY_EXPIRED(HttpStatus.UNPROCESSABLE_ENTITY, "WARRANTY_EXPIRED", "Yêu cầu đã hết hạn bảo hành"),
    WARRANTY_CLAIM_EXISTS(HttpStatus.CONFLICT, "WARRANTY_CLAIM_EXISTS", "Yêu cầu này đang có phiếu bảo hành chưa hoàn tất"),
    INVALID_SCHEDULE(HttpStatus.BAD_REQUEST, "INVALID_SCHEDULE", "Lịch hẹn không hợp lệ"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Lỗi hệ thống"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi cơ sở dữ liệu"),
    EXTERNAL_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "EXTERNAL_SERVICE_ERROR", "Lỗi dịch vụ bên ngoài"),
    FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_UPLOAD_ERROR", "Lỗi tải file"),
    CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CONFIGURATION_ERROR", "Lỗi cấu hình");

    private final org.springframework.http.HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(org.springframework.http.HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}