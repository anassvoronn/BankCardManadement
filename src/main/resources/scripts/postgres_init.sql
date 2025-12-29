
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
VALUES
('4mcnd6Ddkhy8LLDQtIw4fg==', 'user1', current_date + interval '3 year', 'ACTIVE', 1000.00, (SELECT id FROM users WHERE username='user1')),
('YkaD9hvvfnRV3y9PF05q4A==', 'user1', current_date + interval '3 year', 'ACTIVE', 1200.00, (SELECT id FROM users WHERE username='user1')),

('I9WMNKhO912qiVRGv2GQkQ==', 'user2', current_date + interval '3 year', 'ACTIVE', 1000.00, (SELECT id FROM users WHERE username='user2')),
('Cknrj75WEpEiaz8iCUFBjQ==', 'user2', current_date + interval '3 year', 'ACTIVE', 1200.00, (SELECT id FROM users WHERE username='user2')),

('+qUjz7R8lxsBXL8cSy6Utg==', 'user3', current_date + interval '3 year', 'ACTIVE', 1000.00, (SELECT id FROM users WHERE username='user3')),
('YYEAU9uuNk1xmnQwgjwvyw==', 'user3', current_date + interval '3 year', 'ACTIVE', 1200.00, (SELECT id FROM users WHERE username='user3')),

('36qaXdOeMRllJSm7/thgZA==', 'user4', current_date + interval '3 year', 'ACTIVE', 1000.00, (SELECT id FROM users WHERE username='user4')),
('B//0grqdwmESaMFwQZuvYA==', 'user4', current_date + interval '3 year', 'ACTIVE', 1200.00, (SELECT id FROM users WHERE username='user4')),

('g7Qj70ActDD1Fy3pGx/XUA==', 'user5', current_date + interval '3 year', 'ACTIVE', 1000.00, (SELECT id FROM users WHERE username='user5')),
('NOYkShhofFgCo0u6mkC6Qw==', 'user5', current_date + interval '3 year', 'ACTIVE', 1200.00, (SELECT id FROM users WHERE username='user5')),

('TMaw+cSSICiCh7g7vxVg4Q==', 'user6', current_date + interval '3 year', 'ACTIVE', 1000.00, (SELECT id FROM users WHERE username='user6')),
('KozuNlSwZ4AQTDxa1r3JpA==', 'user6', current_date + interval '3 year', 'ACTIVE', 1200.00, (SELECT id FROM users WHERE username='user6')),

('5WLBNNDx+oGV0ZW+dNps1Q==', 'user7', current_date + interval '3 year', 'ACTIVE', 1000.00, (SELECT id FROM users WHERE username='user7')),
('lha3x90xXQpaDz4zcLlB0A==', 'user7', current_date + interval '3 year', 'ACTIVE', 1200.00, (SELECT id FROM users WHERE username='user7')),

('RzAjs/qUgcCDsP+3bCI2rw==', 'user8', current_date + interval '3 year', 'ACTIVE', 1000.00, (SELECT id FROM users WHERE username='user8')),
('AHscB5Y6dcUBzBlrOPimIQ==', 'user8', current_date + interval '3 year', 'ACTIVE', 1200.00, (SELECT id FROM users WHERE username='user8')),

('WkZ0nOeRB1Gc8aFbKlGA0w==', 'user9', current_date + interval '3 year', 'ACTIVE', 1000.00, (SELECT id FROM users WHERE username='user9')),
('kFoUOQaljof5f4Jh77Y5oA==', 'user9', current_date + interval '3 year', 'ACTIVE', 1200.00, (SELECT id FROM users WHERE username='user9')),

('0qfqxJJtPWZ+gAOBYaNsLw==', 'user10', current_date + interval '3 year', 'ACTIVE', 1000.00, (SELECT id FROM users WHERE username='user10')),
('qrGOgpB4V8cosWktlZif7Q==', 'user10', current_date + interval '3 year', 'ACTIVE', 1200.00, (SELECT id FROM users WHERE username='user10'));