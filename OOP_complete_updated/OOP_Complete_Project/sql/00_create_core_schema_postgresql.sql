CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(50),
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    age INTEGER,
    profile_photo VARCHAR(255),
    total_points NUMERIC(10,2) DEFAULT 0.00,
    raw_bottle_count INTEGER DEFAULT 0,
    account_status VARCHAR(20) DEFAULT 'active',
    failed_login_attempts INTEGER DEFAULT 0,
    session_token VARCHAR(255),
    last_activity TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS badges (
    badge_id SERIAL PRIMARY KEY,
    badge_name VARCHAR(50) NOT NULL,
    level INTEGER NOT NULL,
    bonus_points NUMERIC(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS bottle_records (
    record_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE DEFERRABLE,
    bottles_collected INTEGER NOT NULL,
    collection_date DATE NOT NULL,
    week_start_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS coupons (
    coupon_id SERIAL PRIMARY KEY,
    coupon_name VARCHAR(100) NOT NULL,
    points_required NUMERIC(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS inout_logs (
    log_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    performed_at TIMESTAMP DEFAULT NOW(),
    ip_address VARCHAR(45),
    notes VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS points_ledger (
    ledger_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE DEFERRABLE,
    points_change NUMERIC(10,2) NOT NULL,
    source VARCHAR(30) NOT NULL,
    ref_id INTEGER,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS redemptions (
    redemption_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE DEFERRABLE,
    coupon_id INTEGER NOT NULL REFERENCES coupons(coupon_id) ON DELETE RESTRICT DEFERRABLE,
    coupon_code VARCHAR(100) NOT NULL UNIQUE,
    redemption_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'pending'
);

CREATE TABLE IF NOT EXISTS streaks (
    streak_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE DEFERRABLE,
    streak_days INTEGER NOT NULL,
    bonus_points NUMERIC(10,2) NOT NULL,
    date_logged TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_badges (
    user_badge_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE DEFERRABLE,
    badge_id INTEGER NOT NULL REFERENCES badges(badge_id) ON DELETE CASCADE DEFERRABLE,
    date_awarded DATE NOT NULL,
    week_start_date DATE NOT NULL
);

INSERT INTO badges (badge_name, level, bonus_points)
VALUES
    ('Bronze', 1, 0),
    ('Silver', 2, 1),
    ('Emerald', 3, 3),
    ('Gold', 4, 5),
    ('Constellation', 5, 10)
ON CONFLICT DO NOTHING;

INSERT INTO coupons (coupon_name, points_required)
VALUES
    ('School Supplies', 10),
    ('Snack Voucher V1', 30),
    ('Snack Voucher V2', 50),
    ('Lunch Voucher', 100)
ON CONFLICT DO NOTHING;
