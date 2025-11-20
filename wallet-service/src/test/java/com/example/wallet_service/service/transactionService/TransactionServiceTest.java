package com.example.wallet_service.service.transactionService;

import com.example.wallet_service.config.walletConfig.WalletConfig;
import com.example.wallet_service.dto.transactionDto.TransactionDTO;
import com.example.wallet_service.exception.WalletIdNotFoundException;
import com.example.wallet_service.model.transaction.Transaction;
import com.example.wallet_service.model.wallet.Wallet;
import com.example.wallet_service.repository.transactionRepository.TransactionRepository;
import com.example.wallet_service.repository.walletRepository.WalletRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
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

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        // IMPORTANT (Option A): mock all limits
        when(walletConfig.getDailyCreditLimit()).thenReturn(BigDecimal.valueOf(50000));
        when(walletConfig.getMonthlyCreditLimit()).thenReturn(BigDecimal.valueOf(200000));
        when(walletConfig.getDailyDebitLimit()).thenReturn(BigDecimal.valueOf(50000));
        when(walletConfig.getMonthlyDebitLimit()).thenReturn(BigDecimal.valueOf(200000));
    }

    // ======================================================================
    // getTransactionsByWallet
    // ======================================================================

    @Test
    void testGetTransactionsByWallet_Success() {
        Wallet wallet = new Wallet();
        wallet.setId(1L);

        Transaction t = new Transaction();
        t.setId(100L);
        t.setAmount(BigDecimal.TEN);
        t.setWallet(wallet);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletId(1L))
                .thenReturn(List.of(t));

        List<TransactionDTO> result = transactionService.getTransactionsByWallet(1L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }

    @Test
    void testGetTransactionsByWallet_NotFound() {
        when(walletRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(WalletIdNotFoundException.class,
                () -> transactionService.getTransactionsByWallet(999L));
    }

    // ======================================================================
    // getTransactionById
    // ======================================================================

    @Test
    void testGetTransactionById_Found() {
        Transaction t = new Transaction();
        t.setId(7L);

        when(transactionRepository.findById(7L)).thenReturn(Optional.of(t));

        Optional<Transaction> result = transactionService.getTransactionById(7L);

        assertTrue(result.isPresent());
        assertEquals(7L, result.get().getId());
    }

    @Test
    void testGetTransactionById_NotFound() {
        when(transactionRepository.findById(500L))
                .thenReturn(Optional.empty());

        Optional<Transaction> result = transactionService.getTransactionById(500L);

        assertTrue(result.isEmpty());
    }

    // ======================================================================
    // getUserTransactions
    // ======================================================================

    @Test
    void testGetUserTransactions_Valid() {
        Transaction t = new Transaction();
        t.setId(10L);

        when(transactionRepository.findUserTransactionsBetweenDates(anyLong(), any(), any(), any()))
                .thenReturn(List.of(t));

        Object result = transactionService.getUserTransactions(
                1L, "01-01-2024", "02-01-2024", "CREDIT");

        assertTrue(result instanceof List<?>);
        assertEquals(1, ((List<?>) result).size());
    }

    @Test
    void testGetUserTransactions_InvalidDate() {
        Object result = transactionService.getUserTransactions(
                1L, "BAD_DATE", "02-01-2024", "CREDIT");

        assertEquals("Invalid date format. Please use dd-MM-yyyy.", result);
    }

    @Test
    void testGetUserTransactions_InvalidType() {
        Object result = transactionService.getUserTransactions(
                1L, "01-01-2024", "02-01-2024", "XYZ");

        assertEquals("Invalid transaction type. Use CREDIT, DEBIT, or TRANSFER.", result);
    }

    // ======================================================================
    // getTransactionHistory
    // ======================================================================

    @Test
    void testGetTransactionHistory_Success() {
        Transaction t = new Transaction();
        t.setId(55L);

        Page<Transaction> page = new PageImpl<>(List.of(t));

        when(transactionRepository.findTransactionsWithFilters(
                anyLong(), any(), any(), any(), any()))
                .thenReturn(page);

        Page<TransactionDTO> result =
                transactionService.getTransactionHistory(
                        1L, null, "01-01-2024", "02-01-2024", 0, 10
                );

        assertEquals(1, result.getContent().size());
        assertEquals(55L, result.getContent().get(0).getId());
    }

    @Test
    void testGetTransactionHistory_InvalidDate() {
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.getTransactionHistory(
                        1L, null, "BAD", "02-01-2024", 0, 10));
    }

    @Test
    void testGetTransactionHistory_InvalidType() {
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.getTransactionHistory(
                        1L, "WRONG", "01-01-2024", "02-01-2024", 0, 10));
    }

    // ======================================================================
    // validateTransactionLimits
    // ======================================================================

    @Test
    void testValidateTransactionLimits_DailyExceeded() {

        // FIRST call = daily total
        when(transactionRepository.getTotalAmountByWalletAndTypeBetweenDates(anyLong(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(60000)) // daily
                .thenReturn(BigDecimal.valueOf(10000)); // monthly

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.validateTransactionLimits(
                        1L, BigDecimal.valueOf(2000), Transaction.Type.DEBIT));
    }

    @Test
    void testValidateTransactionLimits_MonthlyExceeded() {

        when(transactionRepository.getTotalAmountByWalletAndTypeBetweenDates(anyLong(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(100))     // daily
                .thenReturn(BigDecimal.valueOf(300000)); // monthly

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.validateTransactionLimits(
                        1L, BigDecimal.valueOf(2000), Transaction.Type.DEBIT));
    }

    @Test
    void testValidateTransactionLimits_Valid() {

        when(transactionRepository.getTotalAmountByWalletAndTypeBetweenDates(anyLong(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(100)) // daily
                .thenReturn(BigDecimal.valueOf(500)); // monthly

        assertDoesNotThrow(() ->
                transactionService.validateTransactionLimits(
                        1L, BigDecimal.valueOf(2000), Transaction.Type.CREDIT));
    }

    // ======================================================================
    // createTransaction  (Option A ⇒ validateTransactionLimits must run)
    // ======================================================================

    @Test
    void testCreateTransaction_WalletNotFound() {
        when(walletRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(WalletIdNotFoundException.class,
                () -> transactionService.createTransaction(999L, BigDecimal.TEN, Transaction.Type.CREDIT));
    }

    @Test
    void testCreateTransaction_Success() {

        Wallet wallet = new Wallet();
        wallet.setId(1L);

        // mock findById
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        // ensure no limit violation
        when(transactionRepository.getTotalAmountByWalletAndTypeBetweenDates(anyLong(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO)
                .thenReturn(BigDecimal.ZERO);

        Transaction saved = new Transaction();
        saved.setId(123L);
        saved.setAmount(BigDecimal.TEN);
        saved.setWallet(wallet);
        saved.setTimestamp(LocalDateTime.now());

        when(transactionRepository.save(any())).thenReturn(saved);

        TransactionDTO dto =
                transactionService.createTransaction(1L, BigDecimal.TEN, Transaction.Type.CREDIT);

        assertEquals(123L, dto.getId());
        assertEquals(BigDecimal.TEN, dto.getAmount());
    }
}
