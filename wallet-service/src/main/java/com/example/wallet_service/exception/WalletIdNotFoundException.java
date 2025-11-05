package com.example.wallet_service.exception;

public class WalletIdNotFoundException extends RuntimeException {
    public WalletIdNotFoundException(String message) {
        super(message);
    }

    public WalletIdNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public WalletIdNotFoundException(Throwable cause) {
        super(cause);
    }
}
