package com.example.wallet_service.exception;

public class WalletBlacklistedException extends RuntimeException {
    private final String errorCode;

    public WalletBlacklistedException(String message) {
        super(message);
        this.errorCode = "WALLET_BLACKLISTED";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
