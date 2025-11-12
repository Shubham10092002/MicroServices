-- Drop and recreate the users table

DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL DEFAULT "USER",
    blacklisted  BOOLEAN NOT NULL DEFAULT false
);

-- Insert sample users
INSERT INTO users (username, password, role, blacklisted) VALUES ('shubham', '$2a$10$/d2ImF6VIzmfOGJksiE1YeZIzEPcdfchWVTGFaPK0mT7rZGCl1Xu.','USER', FALSE);
INSERT INTO users (username, password, role, blacklisted) VALUES ('alice', '$2a$10$It9tKsnLnLxI8TuRFFLS7.3aqrTa7vCttkTMZLUv097TE6e5EXhqO', 'USER', FALSE);
INSERT INTO users (username, password, role, blacklisted) VALUES ('bob', '$2a$10$Vi7OrzEygxLLjkxNbZOU4u0wnQpdvx2J8AXjClyKmqgxK5nE3wYOK', 'ADMIN', FALSE);
