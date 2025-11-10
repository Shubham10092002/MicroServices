package com.example.wallet_service.service;

import com.example.wallet_service.config.WalletConfig;
import com.example.wallet_service.data.WalletOperationResult;
import com.example.wallet_service.dto.TransactionSummaryDTO;
import com.example.wallet_service.model.Transaction;
import com.example.wallet_service.model.Wallet;
import com.example.wallet_service.repository.TransactionRepository;
import com.example.wallet_service.repository.WalletRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletConfig walletConfig;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private WalletService walletService;

    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setWalletName("Default Wallet");
        testWallet.setBalance(BigDecimal.valueOf(500));
        testWallet.setUserId(100L);

        when(walletConfig.getMaxCreditLimit()).thenReturn(BigDecimal.valueOf(1000));
        when(walletConfig.getMaxDebitLimit()).thenReturn(BigDecimal.valueOf(500));
    }

    // ============================================================
    // 🔹 CREDIT TESTS
    // ============================================================
    @Nested
    @DisplayName("Credit Operation Tests")
    class CreditTests {

        @Test
        @DisplayName("✅ should credit wallet successfully")
        void shouldCreditWalletSuccessfully() {
            when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));

            WalletOperationResult result = walletService.credit(1L, BigDecimal.valueOf(200), "Deposit");

            assertThat(result).isInstanceOf(WalletOperationResult.Success.class);
            verify(walletRepository).save(any(Wallet.class));
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("❌ should fail if amount is null or <= 0")
        void shouldFailForInvalidCreditAmount() {
            WalletOperationResult result = walletService.credit(1L, BigDecimal.ZERO, "Deposit");
            assertThat(result).isInstanceOf(WalletOperationResult.Failure.class);
            assertThat(((WalletOperationResult.Failure) result).errorCode()).isEqualTo("INVALID_AMOUNT");
        }

        @Test
        @DisplayName("❌ should fail if wallet not found")
        void shouldFailIfWalletNotFound() {
            when(walletRepository.findById(99L)).thenReturn(Optional.empty());

            WalletOperationResult result = walletService.credit(99L, BigDecimal.valueOf(200), "Deposit");
            assertThat(result).isInstanceOf(WalletOperationResult.Failure.class);
            assertThat(((WalletOperationResult.Failure) result).errorCode()).isEqualTo("WALLET_NOT_FOUND");
        }

        @Test
        @DisplayName("❌ should fail if exceeds max credit limit")
        void shouldFailIfExceedsCreditLimit() {
            when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
            when(walletConfig.getMaxCreditLimit()).thenReturn(BigDecimal.valueOf(100));

            WalletOperationResult result = walletService.credit(1L, BigDecimal.valueOf(200), "Deposit");
            assertThat(result).isInstanceOf(WalletOperationResult.Failure.class);
            assertThat(((WalletOperationResult.Failure) result).errorCode()).isEqualTo("LIMIT_EXCEEDED");
        }

        @Test
        @DisplayName("❌ should handle OptimisticLockException gracefully")
        void shouldHandleOptimisticLockException() {
            when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
            doThrow(new OptimisticLockException()).when(walletRepository).save(any(Wallet.class));

            WalletOperationResult result = walletService.credit(1L, BigDecimal.valueOf(50), "Deposit");
            assertThat(result).isInstanceOf(WalletOperationResult.Failure.class);
            assertThat(((WalletOperationResult.Failure) result).errorCode()).isEqualTo("CONFLICT");
        }
    }

    // ============================================================
    // 🔹 DEBIT TESTS
    // ============================================================
    @Nested
    @DisplayName("Debit Operation Tests")
    class DebitTests {

        @Test
        @DisplayName("✅ should debit wallet successfully")
        void shouldDebitWalletSuccessfully() {
            when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));

            WalletOperationResult result = walletService.debit(1L, BigDecimal.valueOf(100), "Purchase");
            assertThat(result).isInstanceOf(WalletOperationResult.Success.class);
            verify(walletRepository).save(any(Wallet.class));
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("❌ should fail for invalid debit amount")
        void shouldFailForInvalidDebitAmount() {
            WalletOperationResult result = walletService.debit(1L, BigDecimal.ZERO, "Invalid");
            assertThat(result).isInstanceOf(WalletOperationResult.Failure.class);
            assertThat(((WalletOperationResult.Failure) result).errorCode()).isEqualTo("INVALID_AMOUNT");
        }

        @Test
        @DisplayName("❌ should fail if wallet not found")
        void shouldFailIfWalletNotFound() {
            when(walletRepository.findById(1L)).thenReturn(Optional.empty());
            WalletOperationResult result = walletService.debit(1L, BigDecimal.valueOf(100), "Purchase");
            assertThat(result).isInstanceOf(WalletOperationResult.Failure.class);
            assertThat(((WalletOperationResult.Failure) result).errorCode()).isEqualTo("WALLET_NOT_FOUND");
        }

        @Test
        @DisplayName("❌ should fail if exceeds max debit limit")
        void shouldFailIfExceedsDebitLimit() {
            when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
            when(walletConfig.getMaxDebitLimit()).thenReturn(BigDecimal.valueOf(50));

            WalletOperationResult result = walletService.debit(1L, BigDecimal.valueOf(100), "Purchase");
            assertThat(result).isInstanceOf(WalletOperationResult.Failure.class);
            assertThat(((WalletOperationResult.Failure) result).errorCode()).isEqualTo("LIMIT_EXCEEDED");
        }

        @Test
        @DisplayName("❌ should fail for insufficient funds")
        void shouldFailForInsufficientFunds() {
            testWallet.setBalance(BigDecimal.valueOf(100));
            when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));

            WalletOperationResult result = walletService.debit(1L, BigDecimal.valueOf(200), "Purchase");
            assertThat(result).isInstanceOf(WalletOperationResult.Failure.class);
            assertThat(((WalletOperationResult.Failure) result).errorCode()).isEqualTo("INSUFFICIENT_FUNDS");
        }

        @Test
        @DisplayName("❌ should handle OptimisticLockException on debit")
        void shouldHandleOptimisticLockExceptionOnDebit() {
            when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
            doThrow(new OptimisticLockException()).when(walletRepository).save(any(Wallet.class));

            WalletOperationResult result = walletService.debit(1L, BigDecimal.valueOf(10), "Test");
            assertThat(result).isInstanceOf(WalletOperationResult.Failure.class);
            assertThat(((WalletOperationResult.Failure) result).errorCode()).isEqualTo("CONFLICT");
        }
    }

    // ============================================================
    // 🔹 BALANCE CHECK
    // ============================================================
    @Nested
    @DisplayName("Balance Check Tests")
    class BalanceTests {

        @Test
        @DisplayName("✅ should return balance successfully")
        void shouldReturnBalanceSuccessfully() {
            when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));

            WalletOperationResult result = walletService.getBalance(1L);
            assertThat(result).isInstanceOf(WalletOperationResult.Balance.class);
        }

        @Test
        @DisplayName("❌ should fail if wallet not found for balance")
        void shouldFailIfWalletNotFoundForBalance() {
            when(walletRepository.findById(1L)).thenReturn(Optional.empty());

            WalletOperationResult result = walletService.getBalance(1L);
            assertThat(result).isInstanceOf(WalletOperationResult.Failure.class);
            assertThat(((WalletOperationResult.Failure) result).errorCode()).isEqualTo("WALLET_NOT_FOUND");
        }
    }

    // ============================================================
    // 🔹 READ METHODS
    // ============================================================
    @Test
    @DisplayName("✅ should get all wallets")
    void shouldGetAllWallets() {
        when(walletRepository.findAll()).thenReturn(List.of(testWallet));
        assertThat(walletService.getAllWallets()).hasSize(1);
    }

    @Test
    @DisplayName("✅ should get wallet by ID")
    void shouldGetWalletById() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        assertThat(walletService.getWalletById(1L)).isPresent();
    }
 // make sure it's imported

    @Test
    @DisplayName("✅ should handle getTransactionSummary properly")
    void shouldGetTransactionSummary() {
        List<Object[]> mockResult = Collections.singletonList(
                new Object[]{Transaction.Type.CREDIT, BigDecimal.valueOf(500)}
        );

        when(transactionRepository.getTransactionSumsByType(anyLong()))
                .thenReturn(mockResult);

        List<TransactionSummaryDTO> summary = walletService.getTransactionSummary(1L);

        assertThat(summary).hasSize(1);
        assertThat(summary.get(0).type()).isEqualTo(Transaction.Type.CREDIT);
    }



}
