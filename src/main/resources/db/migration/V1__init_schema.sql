    -- =====================================================================
-- ThoNha - Repair service marketplace
-- Database schema (MySQL 8 / InnoDB), generated from ThoNha_EN.drawio
-- NOTE: enum values, VARCHAR lengths and DECIMAL precisions are not in the
--       diagram; they are sensible defaults - adjust to your needs.
-- =====================================================================

-- Select your target database in the client before running this script
-- (or run these two lines first, separately):
--   CREATE DATABASE IF NOT EXISTS thonha CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--   USE thonha;

SET
FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 1. Users & roles
-- ---------------------------------------------------------------------
CREATE TABLE users
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    full_name    VARCHAR(255) NOT NULL,
    username     VARCHAR(100) NULL,
    email        VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20)  NOT NULL,
    password     VARCHAR(255) NOT NULL,
    status       ENUM('active','inactive','banned') NOT NULL DEFAULT 'active',
    avatar_url   VARCHAR(500) NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_phone_number (phone_number),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB;

CREATE TABLE roles
(
    id   INT         NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role_id INT    NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(64)  NOT NULL,
    expires_at DATETIME     NOT NULL,
    revoked_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE address
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    label        VARCHAR(100) NULL,
    full_address VARCHAR(500) NOT NULL,
    lat          DECIMAL(10, 7) NULL,
    lng          DECIMAL(10, 7) NULL,
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. Reference tables
-- ---------------------------------------------------------------------
CREATE TABLE service_category
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(255) NOT NULL,
    description   VARCHAR(500) NULL,
    icon          VARCHAR(255) NULL,
    status        ENUM('active','inactive') NOT NULL DEFAULT 'active',
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE promotion
(
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    promo_code    VARCHAR(50)    NOT NULL,
    discount_type ENUM('percent','fixed') NOT NULL,
    `value`       DECIMAL(15, 2) NOT NULL,
    quantity      INT            NOT NULL DEFAULT 0,
    used_quantity INT            NOT NULL DEFAULT 0,
    start_date    DATE           NOT NULL,
    end_date      DATE           NOT NULL,
    status        ENUM('active','inactive','expired') NOT NULL DEFAULT 'active',
    PRIMARY KEY (id),
    UNIQUE KEY uk_promotion_promo_code (promo_code)
) ENGINE=InnoDB;

CREATE TABLE bank_account
(
    id             INT          NOT NULL AUTO_INCREMENT,
    bank_name      VARCHAR(255) NOT NULL,
    account_number VARCHAR(50)  NOT NULL,
    account_holder VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. Workers
-- ---------------------------------------------------------------------
CREATE TABLE worker_profile
(
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    user_id             BIGINT        NOT NULL,
    service_area        VARCHAR(255) NULL,
    approval_status     ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
    availability_status ENUM('online','offline','busy')       NOT NULL DEFAULT 'offline',
    average_rating      DECIMAL(3, 2) NOT NULL DEFAULT 0,
    acceptance_rate     DECIMAL(5, 2) NOT NULL DEFAULT 0,
    active_job_count    INT           NOT NULL DEFAULT 0,
    residence_city      VARCHAR(255) NULL,
    years_of_experience INT NULL,
    bank_account_id     INT NULL,
    reviewed_at         DATETIME NULL,
    reviewed_by         BIGINT NULL,
    reject_reason       VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_worker_profile_user (user_id),
    CONSTRAINT fk_worker_profile_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_worker_profile_bank FOREIGN KEY (bank_account_id) REFERENCES bank_account (id),
    CONSTRAINT fk_worker_profile_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE worker_specialty
(
    worker_profile_id    BIGINT NOT NULL,
    service_category_id  BIGINT NOT NULL,
    PRIMARY KEY (worker_profile_id, service_category_id),
    CONSTRAINT fk_specialty_profile FOREIGN KEY (worker_profile_id) REFERENCES worker_profile (id) ON DELETE CASCADE,
    CONSTRAINT fk_specialty_category FOREIGN KEY (service_category_id) REFERENCES service_category (id)
) ENGINE=InnoDB;

CREATE TABLE worker_schedule
(
    id                BIGINT NOT NULL AUTO_INCREMENT,
    worker_profile_id BIGINT NOT NULL,
    apply_type        ENUM('specific_date','weekly') NOT NULL,
    specific_date     DATE NULL,
    day_of_week       TINYINT NULL COMMENT '1=Monday ... 7=Sunday',
    start_time        TIME   NOT NULL,
    end_time          TIME   NOT NULL,
    status            ENUM('available','off') NOT NULL DEFAULT 'available',
    PRIMARY KEY (id),
    CONSTRAINT fk_schedule_profile FOREIGN KEY (worker_profile_id) REFERENCES worker_profile (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE worker_document
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    worker_profile_id BIGINT       NOT NULL,
    type              ENUM('id_card','certificate','other') NOT NULL,
    url               VARCHAR(500) NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_document_profile FOREIGN KEY (worker_profile_id) REFERENCES worker_profile (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. Wallet
-- ---------------------------------------------------------------------
CREATE TABLE wallet
(
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    worker_id  BIGINT         NOT NULL,
    balance    DECIMAL(15, 2) NOT NULL DEFAULT 0,
    updated_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_worker (worker_id),
    CONSTRAINT fk_wallet_worker FOREIGN KEY (worker_id) REFERENCES worker_profile (id)
) ENGINE=InnoDB;

CREATE TABLE withdrawal_request
(
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    wallet_id    BIGINT         NOT NULL,
    amount       DECIMAL(15, 2) NOT NULL,
    status       ENUM('pending','approved','rejected','completed') NOT NULL DEFAULT 'pending',
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_withdrawal_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. Repair requests
-- ---------------------------------------------------------------------
CREATE TABLE repair_request
(
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    request_code           VARCHAR(50) NOT NULL,
    customer_id            BIGINT      NOT NULL,
    category_id            BIGINT      NOT NULL,
    address_id             BIGINT NULL,
    worker_id              BIGINT NULL,
    description            TEXT NULL,
    priority_level         ENUM('normal','urgent') NOT NULL DEFAULT 'normal',
    address_text           VARCHAR(500) NULL,
    lat                    DECIMAL(10, 7) NULL,
    lng                    DECIMAL(10, 7) NULL,
    status                 ENUM('pending','matching','assigned','in_progress','completed','cancelled') NOT NULL DEFAULT 'pending',
    final_price            DECIMAL(15, 2) NULL,
    created_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ai_suggested_price_min DECIMAL(15, 2) NULL,
    ai_analysis            TEXT NULL,
    ai_suggested_price_max DECIMAL(15, 2) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_repair_request_code (request_code),
    CONSTRAINT fk_request_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_request_category FOREIGN KEY (category_id) REFERENCES service_category (id),
    CONSTRAINT fk_request_address FOREIGN KEY (address_id) REFERENCES address (id) ON DELETE SET NULL,
    CONSTRAINT fk_request_worker FOREIGN KEY (worker_id) REFERENCES worker_profile (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE request_attachment
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    request_id BIGINT       NOT NULL,
    type       VARCHAR(20) NOT NULL,
    url        VARCHAR(500) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_attachment_request FOREIGN KEY (request_id) REFERENCES repair_request (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE quotation
(
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    request_id   BIGINT         NOT NULL,
    worker_id    BIGINT         NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    notes        TEXT NULL,
    status       ENUM('pending','accepted','rejected') NOT NULL DEFAULT 'pending',
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_quotation_request FOREIGN KEY (request_id) REFERENCES repair_request (id),
    CONSTRAINT fk_quotation_worker FOREIGN KEY (worker_id) REFERENCES worker_profile (id)
) ENGINE=InnoDB;

CREATE TABLE matching_log
(
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    request_id            BIGINT NOT NULL,
    worker_id             BIGINT NOT NULL,
    score                 DECIMAL(6, 2) NULL,
    distance_score        DECIMAL(6, 2) NULL,
    rating_score          DECIMAL(6, 2) NULL,
    acceptance_rate_score DECIMAL(6, 2) NULL,
    workload_score        DECIMAL(6, 2) NULL,
    result                ENUM('pending','accepted','rejected','timeout') NOT NULL DEFAULT 'pending',
    sent_time             DATETIME NULL,
    response_time         DATETIME NULL,
    note                  TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_matching_request FOREIGN KEY (request_id) REFERENCES repair_request (id),
    CONSTRAINT fk_matching_worker FOREIGN KEY (worker_id) REFERENCES worker_profile (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. Payments
-- ---------------------------------------------------------------------
CREATE TABLE `transaction`
(
    id                     BIGINT         NOT NULL AUTO_INCREMENT,
    request_id             BIGINT         NOT NULL,
    promotion_id           BIGINT NULL,
    amount                 DECIMAL(15, 2) NOT NULL,
    payment_method         ENUM('cash','bank_transfer','e_wallet','card') NOT NULL,
    commission_rate        DECIMAL(5, 2)  NOT NULL,
    commission_amount      DECIMAL(15, 2) NOT NULL,
    worker_earning         DECIMAL(15, 2) NOT NULL,
    status                 ENUM('pending','success','failed','refunded') NOT NULL DEFAULT 'pending',
    gateway_transaction_id VARCHAR(100) NULL,
    created_at             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_transaction_request FOREIGN KEY (request_id) REFERENCES repair_request (id),
    CONSTRAINT fk_transaction_promotion FOREIGN KEY (promotion_id) REFERENCES promotion (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. Reviews & complaints
-- ---------------------------------------------------------------------
CREATE TABLE review
(
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    request_id  BIGINT   NOT NULL,
    customer_id BIGINT   NOT NULL,
    worker_id   BIGINT   NOT NULL,
    star_rating TINYINT  NOT NULL,
    `comment`   TEXT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_review_star_rating CHECK (star_rating BETWEEN 1 AND 5),
    CONSTRAINT fk_review_request FOREIGN KEY (request_id) REFERENCES repair_request (id),
    CONSTRAINT fk_review_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_review_worker FOREIGN KEY (worker_id) REFERENCES worker_profile (id)
) ENGINE=InnoDB;

CREATE TABLE complaint
(
    id                BIGINT   NOT NULL AUTO_INCREMENT,
    request_id        BIGINT   NOT NULL,
    handling_admin_id BIGINT NULL,
    reason            ENUM('poor_quality','overcharge','no_show','other') NOT NULL,
    description       TEXT NULL,
    status            ENUM('open','processing','resolved','rejected') NOT NULL DEFAULT 'open',
    resolution        TEXT NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_complaint_request FOREIGN KEY (request_id) REFERENCES repair_request (id),
    CONSTRAINT fk_complaint_admin FOREIGN KEY (handling_admin_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE complaint_image
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    complaint_id BIGINT       NOT NULL,
    url          VARCHAR(500) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_complaint_image_complaint FOREIGN KEY (complaint_id) REFERENCES complaint (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 8. Messaging & notifications
-- ---------------------------------------------------------------------
CREATE TABLE message
(
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    request_id BIGINT   NOT NULL,
    sender_id  BIGINT   NOT NULL,
    content    TEXT     NOT NULL,
    is_seen    BOOLEAN  NOT NULL DEFAULT FALSE,
    sent_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_message_request FOREIGN KEY (request_id) REFERENCES repair_request (id) ON DELETE CASCADE,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE message_image
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    message_id BIGINT       NOT NULL,
    url        VARCHAR(500) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_message_image_message FOREIGN KEY (message_id) REFERENCES message (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE notification
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    type       ENUM('request','quotation','message','payment','system') NOT NULL,
    content    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

SET
FOREIGN_KEY_CHECKS = 1;