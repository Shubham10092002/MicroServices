package com.example.wallet_service.service;

import com.example.wallet_service.config.WalletConfig;
import com.example.wallet_service.dto.TransactionDTO;
import com.example.wallet_service.exception.WalletIdNotFoundException;
import com.example.wallet_service.model.Transaction;
import com.example.wallet_service.model.Wallet;
import com.example.wallet_service.repository.TransactionRepository;
import com.example.wallet_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletConfig walletConfig;

    @InjectMocks
    private TransactionService transactionService;

    private Wallet wallet;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        wallet = new Wallet();
        wallet.setId(1L);
        wallet.setWalletName("Test Wallet");

        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setWallet(wallet);
        transaction.setAmount(BigDecimal.valueOf(500));
        transaction.setType(Transaction.Type.CREDIT);
        transaction.setTimestamp(LocalDateTime.now());
    }

    // ============================================================
    // ✅ BASIC FETCH TESTS
    // ============================================================
    @Test
    @DisplayName("✅ should fetch all transactions successfully")
    void shouldFetchAllTransactions() {
        when(transactionRepository.findAll()).thenReturn(List.of(transaction));

        List<TransactionDTO> result = transactionService.getAllTransactions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualTo(BigDecimal.valueOf(500));
        verify(transactionRepository).findAll();
    }

    @Test
    @DisplayName("✅ should fetch transactions by wallet ID successfully")
    void shouldFetchTransactionsByWalletId() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletId(1L)).thenReturn(List.of(transaction));

        List<TransactionDTO> result = transactionService.getTransactionsByWallet(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualTo(BigDecimal.valueOf(500));
        verify(transactionRepository).findByWalletId(1L);
    }

    @Test
    @DisplayName("❌ should throw WalletIdNotFoundException if wallet not found")
    void shouldThrowIfWalletNotFound() {
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionsByWallet(1L))
                .isInstanceOf(WalletIdNotFoundException.class)
                .hasMessageContaining("Wallet ID not found");
    }

    // ============================================================
    // ✅ USER TRANSACTION FILTER TESTS
    // ============================================================
    @Nested
    @DisplayName("User Transaction Filter Tests")
    class UserTransactionTests {

        @Test
        @DisplayName("✅ should return transactions between dates for user")
        void shouldReturnUserTransactionsBetweenDates() {
            when(transactionRepository.findUserTransactionsBetweenDates(
                    anyLong(), any(), any(), any()))
                    .thenReturn(List.of(transaction));

            Object result = transactionService.getUserTransactions(1L, "01-11-2025", "10-11-2025", "CREDIT");

            assertThat(result).isInstanceOf(List.class);
            List<?> list = (List<?>) result;
            assertThat(list).hasSize(1);
        }

        @Test
        @DisplayName("❌ should return error for invalid date format")
        void shouldReturnErrorForInvalidDateFormat() {
            Object result = transactionService.getUserTransactions(1L, "invalid-date", "10-11-2025", "CREDIT");
            assertThat(result).isEqualTo("Invalid date format. Please use dd-MM-yyyy.");
        }

        @Test
        @DisplayName("❌ should return error for invalid transaction type")
        void shouldReturnErrorForInvalidTransactionType() {
            Object result = transactionService.getUserTransactions(1L, "01-11-2025", "10-11-2025", "INVALID");
            assertThat(result).isEqualTo("Invalid transaction type. Use CREDIT, DEBIT, or TRANSFER.");
        }
    }

    // ============================================================
    // ✅ PAGINATION TESTS
    // ============================================================
    @Test
    @DisplayName("✅ should fetch paginated transaction history successfully")
    void shouldFetchPaginatedTransactionHistory() {
        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction));
        when(transactionRepository.findTransactionsWithFilters(
                anyLong(), anyLong(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(transactionPage);

        Page<TransactionDTO> result = transactionService.getTransactionHistory(
                1L, 1L, "CREDIT", "01-11-2025", "10-11-2025", 0, 5);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAmount()).isEqualTo(BigDecimal.valueOf(500));
    }

    @Test
    @DisplayName("❌ should throw for invalid transaction type in pagination")
    void shouldThrowForInvalidTransactionTypeInPagination() {
        assertThatThrownBy(() ->
                transactionService.getTransactionHistory(1L, 1L, "INVALID", "01-11-2025", "10-11-2025", 0, 5)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid transaction type");
    }

    @Test
    @DisplayName("❌ should throw for invalid date format in pagination")
    void shouldThrowForInvalidDateFormatInPagination() {
        assertThatThrownBy(() ->
                transactionService.getTransactionHistory(1L, 1L, "CREDIT", "bad-date", "10-11-2025", 0, 5)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid date format");
    }

    // ============================================================
    // ✅ VALIDATION LOGIC TESTS
    // ============================================================
    @Nested
    @DisplayName("Transaction Limit Validation Tests")
    class ValidationTests {

        @BeforeEach
        void setupLimits() {
            when(walletConfig.getDailyDebitLimit()).thenReturn(BigDecimal.valueOf(1000));
            when(walletConfig.getMonthlyDebitLimit()).thenReturn(BigDecimal.valueOf(3000));
            when(walletConfig.getDailyCreditLimit()).thenReturn(BigDecimal.valueOf(2000));
            when(walletConfig.getMonthlyCreditLimit()).thenReturn(BigDecimal.valueOf(5000));
        }

        @Test
        @DisplayName("✅ should pass validation within limits")
        void shouldPassValidationWithinLimits() {
            when(transactionRepository.getTotalAmountByWalletAndTypeBetweenDates(anyLong(), any(), any(), any()))
                    .thenReturn(BigDecimal.valueOf(100));

            assertThatCode(() ->
                    transactionService.validateTransactionLimits(1L, BigDecimal.valueOf(200), Transaction.Type.CREDIT)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("❌ should throw if daily limit exceeded")
        void shouldThrowIfDailyLimitExceeded() {
            when(transactionRepository.getTotalAmountByWalletAndTypeBetweenDates(anyLong(), any(), any(), any()))
                    .thenReturn(BigDecimal.valueOf(2000)); // Already at limit

            assertThatThrownBy(() ->
                    transactionService.validateTransactionLimits(1L, BigDecimal.valueOf(1000), Transaction.Type.CREDIT)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("daily limit exceeded");
        }


        @Test
        @DisplayName("❌ should throw if monthly limit exceeded")
        void shouldThrowIfMonthlyLimitExceeded() {
            when(transactionRepository.getTotalAmountByWalletAndTypeBetweenDates(anyLong(), any(), any(), any()))
                    .thenReturn(BigDecimal.valueOf(100))   // daily total below limit
                    .thenReturn(BigDecimal.valueOf(6000)); // monthly total above limit

            assertThatThrownBy(() ->
                    transactionService.validateTransactionLimits(1L, BigDecimal.valueOf(1000), Transaction.Type.CREDIT)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("monthly limit exceeded");
        }


        @Test
        @DisplayName("❌ should throw for unsupported transaction type")
        void shouldThrowForUnsupportedType() {
            assertThatThrownBy(() ->
                    transactionService.validateTransactionLimits(1L, BigDecimal.valueOf(100), null)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported transaction type");
        }
    }

    // ============================================================
    // ✅ CREATE TRANSACTION TESTS
    // ============================================================
    @Test
    @DisplayName("✅ should create transaction successfully")
    void shouldCreateTransactionSuccessfully() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.getTotalAmountByWalletAndTypeBetweenDates(anyLong(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO)  // daily total
                .thenReturn(BigDecimal.ZERO); // monthly total
        when(walletConfig.getDailyCreditLimit()).thenReturn(BigDecimal.valueOf(2000));
        when(walletConfig.getMonthlyCreditLimit()).thenReturn(BigDecimal.valueOf(5000));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionDTO dto = transactionService.createTransaction(1L, BigDecimal.valueOf(500), Transaction.Type.CREDIT);

        assertThat(dto.getAmount()).isEqualTo(BigDecimal.valueOf(500));
        verify(transactionRepository).save(any(Transaction.class));
    }


    @Test
    @DisplayName("❌ should throw if wallet not found when creating transaction")
    void shouldThrowIfWalletNotFoundWhenCreatingTransaction() {
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                transactionService.createTransaction(1L, BigDecimal.valueOf(500), Transaction.Type.CREDIT)
        ).isInstanceOf(WalletIdNotFoundException.class)
                .hasMessageContaining("Wallet ID not found");
    }
}
