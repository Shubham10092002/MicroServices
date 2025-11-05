-- Drop and recreate the users table

DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL DEFAULT "USER"
);

-- Insert sample users
INSERT INTO users (username, password, role) VALUES ('shubham', 'password123',"USER");
INSERT INTO users (username, password, role) VALUES ('alice', 'alicepass', "USER");
INSERT INTO users (username, password, role) VALUES ('bob', 'bobpass', "ADMIN");
