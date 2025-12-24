
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    encrypted_number VARCHAR(255) UNIQUE NOT NULL,
    owner_name VARCHAR(255) NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    balance NUMERIC(15, 2) NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY(user_id) REFERENCES users(id)
);

INSERT INTO users (username, password, role) VALUES
('user1', 'password1', 'ADMIN'),
('user2', 'password2', 'ADMIN'),
('user3', 'password3', 'USER'),
('user4', 'password4', 'USER'),
('user5', 'password5', 'USER'),
('user6', 'password6', 'USER'),
('user7', 'password7', 'USER'),
('user8', 'password8', 'USER'),
('user9', 'password9', 'USER'),
('user10', 'password10', 'USER');

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

INSERT INTO cards (encrypted_number, owner_name, expiry_date, status, balance, user_id)
SELECT
    md5(random()::text || clock_timestamp()::text),-- encrypted_number
    username,                                      -- owner_name
    current_date + INTERVAL '3 year',              -- expiry_date
    'ACTIVE',                                      -- status
    1000.00,                                       -- balance
    id                                             -- user_id
FROM users
UNION ALL
SELECT
    md5(random()::text || clock_timestamp()::text),
    username,
    current_date + INTERVAL '4 year',
    'ACTIVE',
    1000.00,
    id
FROM users;