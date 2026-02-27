-- Migration: Tính năng thanh toán VNPay + Hệ thống Xu
-- DB: web_truyen_online | MySQL 8.0 | 2026-02-27

-- Thêm cột is_locked vào chapters
SET @s = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA='web_truyen_online' AND TABLE_NAME='chapters' AND COLUMN_NAME='is_locked');
SET @q = IF(@s=0, 'ALTER TABLE chapters ADD COLUMN is_locked BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1');
PREPARE p FROM @q; EXECUTE p; DEALLOCATE PREPARE p;

-- Thêm cột coins_price vào chapters
SET @s2 = (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA='web_truyen_online' AND TABLE_NAME='chapters' AND COLUMN_NAME='coins_price');
SET @q2 = IF(@s2=0, 'ALTER TABLE chapters ADD COLUMN coins_price INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE p2 FROM @q2; EXECUTE p2; DEALLOCATE PREPARE p2;

-- Gói nạp xu
CREATE TABLE IF NOT EXISTS coin_packages (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)   NOT NULL,
    coins       INT            NOT NULL,
    bonus_coins INT            NOT NULL DEFAULT 0,
    price       DECIMAL(10,2)  NOT NULL,
    is_active   BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  DATETIME       NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ví xu người dùng
CREATE TABLE IF NOT EXISTS user_wallets (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    balance    INT      NOT NULL DEFAULT 0,
    version    INT      NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_wallet (user_id),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Đơn hàng thanh toán
CREATE TABLE IF NOT EXISTS payment_orders (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    order_code         VARCHAR(50)    NOT NULL,
    user_id            BIGINT         NOT NULL,
    package_id         BIGINT         NOT NULL,
    amount             DECIMAL(10,2)  NOT NULL,
    coins_to_add       INT            NOT NULL,
    status             ENUM('PENDING','SUCCESS','FAILED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    vnp_txn_ref        VARCHAR(100)   NULL,
    vnp_transaction_no VARCHAR(100)   NULL,
    created_at         DATETIME       NOT NULL DEFAULT NOW(),
    completed_at       DATETIME       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_order_code (order_code),
    UNIQUE KEY uq_vnp_txn (vnp_transaction_no),
    CONSTRAINT fk_order_user    FOREIGN KEY (user_id)    REFERENCES users(id)         ON DELETE CASCADE,
    CONSTRAINT fk_order_package FOREIGN KEY (package_id) REFERENCES coin_packages(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Lịch sử biến động xu
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    type          ENUM('DEPOSIT','SPEND','BONUS') NOT NULL,
    amount        INT          NOT NULL,
    balance_after INT          NOT NULL,
    description   VARCHAR(255) NULL,
    ref_id        BIGINT       NULL,
    created_at    DATETIME     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_txn_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Chương đã mở khóa
CREATE TABLE IF NOT EXISTS user_chapter_access (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    chapter_id  BIGINT   NOT NULL,
    coins_spent INT      NOT NULL DEFAULT 0,
    accessed_at DATETIME NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_chapter (user_id, chapter_id),
    CONSTRAINT fk_access_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_access_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Dữ liệu mẫu: gói xu
INSERT IGNORE INTO coin_packages (id, name, coins, bonus_coins, price) VALUES
    (1, 'Goi Khoi Dau',   100,    0,  10000.00),
    (2, 'Goi Tieu Chuan', 500,   50,  45000.00),
    (3, 'Goi Pho Bien',  1000,  150,  85000.00),
    (4, 'Goi VIP',       2500,  500, 200000.00),
    (5, 'Goi Kim Cuong', 5500, 1500, 400000.00);

-- Tạo ví xu cho toàn bộ users hiện có
INSERT IGNORE INTO user_wallets (user_id, balance, version)
SELECT id, 0, 0 FROM users;

SELECT 'Migration payment hoan tat!' AS result;
