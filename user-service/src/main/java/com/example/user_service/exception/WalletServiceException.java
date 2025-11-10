package com.example.user_service.exception;

public class WalletServiceException extends RuntimeException {
    private final String errorCode;

    public WalletServiceException(String message) {
        super(message);
        this.errorCode = "WALLET_SERVICE_ERROR";
    }

    public WalletServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
