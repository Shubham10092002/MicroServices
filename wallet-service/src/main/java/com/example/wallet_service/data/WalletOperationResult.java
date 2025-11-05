package com.example.wallet_service.data;
public sealed interface WalletOperationResult
        permits WalletOperationResult.Success, WalletOperationResult.Failure, WalletOperationResult.Balance {

    record Success(String message) implements WalletOperationResult {}
    record Failure(String errorCode, String reason) implements WalletOperationResult {}
    record Balance(Long walletId, String balance) implements WalletOperationResult {}
}

