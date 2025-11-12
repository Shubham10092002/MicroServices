-- Drop and recreate wallet and transaction tables

DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS wallets;

CREATE TABLE wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_name VARCHAR(100),
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0,
    blacklisted BOOLEAN NOT NULL DEFAULT FALSE,

    user_id BIGINT NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    timestamp TIMESTAMP(6),
    CONSTRAINT fk_wallet_transaction FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
);

-- Insert sample wallets"
INSERT INTO wallets (wallet_name, balance,blacklisted, user_id, version)
VALUES ('Default Wallet', 500.00, FALSE, 1, 0),
       ('Savings Wallet', 1500.50, FALSE, 2, 0),
       ('saving Wallet', 10000, 3, FALSE, 0);


-- Insert sample transactions
INSERT INTO transactions (wallet_id, amount, type, description, timestamp)
VALUES (1, 100.00, 'CREDIT', 'Initial Deposit', CURRENT_TIMESTAMP),
       (1, 50.00, 'DEBIT', 'Purchase', CURRENT_TIMESTAMP),
       (2, 500.00, 'CREDIT', 'Transfer from User 1', CURRENT_TIMESTAMP);
