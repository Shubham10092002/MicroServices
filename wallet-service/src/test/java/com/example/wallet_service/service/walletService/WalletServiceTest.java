package com.example.wallet_service.service.walletService;

import com.example.wallet_service.client.userClient.UserClient;
import com.example.wallet_service.config.walletConfig.WalletConfig;
import com.example.wallet_service.data.WalletOperationResult;
import com.example.wallet_service.dto.walletDto.CreateWalletDTO;
import com.example.wallet_service.exception.WalletBlacklistedException;
import com.example.wallet_service.exception.WalletIdNotFoundException;
import com.example.wallet_service.model.transaction.Transaction;
import com.example.wallet_service.model.wallet.Wallet;
import com.example.wallet_service.repository.transactionRepository.TransactionRepository;
import com.example.wallet_service.repository.walletRepository.WalletRepository;
import com.example.wallet_service.service.transactionService.TransactionService;

import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletConfig walletConfig;
    @Mock private TransactionService transactionService;
    @Mock private UserClient userClient;

    @InjectMocks @Spy
    private WalletService walletService;

    Wallet wallet;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        wallet = new Wallet();
        wallet.setId(1L);
        wallet.setUserId(10L);
        wallet.setBalance(BigDecimal.valueOf(1000));
        wallet.setBlacklisted(false);
    }

    // ------------------------------------------------------------
    // createWalletForUser()
    // ------------------------------------------------------------
    @Test
    void testCreateWalletForUser() {
        CreateWalletDTO dto = new CreateWalletDTO("TestWallet", BigDecimal.TEN);

        when(walletRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.createWalletForUser(10L, dto);

        assertEquals(10L, result.getUserId());
        assertEquals("TestWallet", result.getWalletName());
        assertEquals(BigDecimal.TEN, result.getBalance());
    }


    // ------------------------------------------------------------
    // getWalletById
    // ------------------------------------------------------------
    @Test
    void testGetWalletById_Found() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        assertTrue(walletService.getWalletById(1L).isPresent());
    }

    @Test
    void testGetWalletById_NotFound() {
        when(walletRepository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(walletService.getWalletById(99L).isEmpty());
    }

    // ------------------------------------------------------------
    // credit() OPTION A LOGIC
    // ------------------------------------------------------------

    @Test
    void testCredit_InvalidAmount() {
        WalletOperationResult result = walletService.credit(1L, BigDecimal.ZERO, "test");

        assertEquals("INVALID_AMOUNT",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testCredit_WalletNotFound() {
        when(walletRepository.findById(anyLong())).thenReturn(Optional.empty());

        WalletOperationResult result = walletService.credit(1L, BigDecimal.TEN, "test");

        assertEquals("WALLET_NOT_FOUND",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testCredit_WalletBlacklisted_ReturnsFailureUNKNOWN() {
        wallet.setBlacklisted(true);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        WalletOperationResult result =
                walletService.credit(1L, BigDecimal.TEN, "test");

        assertEquals("UNKNOWN_ERROR",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testCredit_UserBlacklisted_ReturnsFailureUNKNOWN() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(userClient.isUserBlacklisted(10L)).thenReturn(true);

        WalletOperationResult result =
                walletService.credit(1L, BigDecimal.TEN, "test");

        assertEquals("UNKNOWN_ERROR",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testCredit_ExceedsConfiguredLimit() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(false);
        when(walletConfig.getMaxCreditLimit()).thenReturn(BigDecimal.ONE);

        WalletOperationResult result =
                walletService.credit(1L, BigDecimal.TEN, "test");

        assertEquals("LIMIT_EXCEEDED",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testCredit_LimitValidationThrows_MapsToFailure() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(false);
        when(walletConfig.getMaxCreditLimit()).thenReturn(BigDecimal.valueOf(5000));

        doThrow(new IllegalArgumentException("limit fail"))
                .when(transactionService).validateTransactionLimits(anyLong(), any(), any());

        WalletOperationResult result =
                walletService.credit(1L, BigDecimal.TEN, "test");

        assertEquals("LIMIT_EXCEEDED",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testCredit_OptimisticLock_MappedToConflict() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(false);
        when(walletConfig.getMaxCreditLimit()).thenReturn(BigDecimal.valueOf(5000));
        doNothing().when(transactionService).validateTransactionLimits(anyLong(), any(), any());

        doThrow(new OptimisticLockException("conflict"))
                .when(walletRepository).save(any());

        WalletOperationResult result =
                walletService.credit(1L, BigDecimal.TEN, "test");

        assertEquals("CONFLICT",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testCredit_UnexpectedError_MapsToUnknown() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(false);
        when(walletConfig.getMaxCreditLimit()).thenReturn(BigDecimal.valueOf(5000));

        doNothing().when(transactionService).validateTransactionLimits(anyLong(), any(), any());
        doThrow(new RuntimeException("boom")).when(transactionRepository).save(any());

        WalletOperationResult result =
                walletService.credit(1L, BigDecimal.TEN, "test");

        assertEquals("UNKNOWN_ERROR",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testCredit_Success() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(false);
        when(walletConfig.getMaxCreditLimit()).thenReturn(BigDecimal.valueOf(5000));
        doNothing().when(transactionService).validateTransactionLimits(anyLong(), any(), any());
        when(walletRepository.save(any())).thenReturn(wallet);
        when(transactionRepository.save(any())).thenReturn(new Transaction());

        WalletOperationResult result =
                walletService.credit(1L, BigDecimal.valueOf(50), "ok");

        assertTrue(result instanceof WalletOperationResult.Success);
    }

    // ------------------------------------------------------------
    // debit() OPTION A LOGIC
    // ------------------------------------------------------------
    @Test
    void testDebit_InvalidAmount() {
        WalletOperationResult result =
                walletService.debit(1L, BigDecimal.ZERO, "test");

        assertEquals("INVALID_AMOUNT",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testDebit_WalletNotFound() {
        when(walletRepository.findById(anyLong())).thenReturn(Optional.empty());

        WalletOperationResult result =
                walletService.debit(1L, BigDecimal.TEN, "test");

        assertEquals("WALLET_NOT_FOUND",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testDebit_BlacklistedWallet_ReturnsFailureUnknown() {
        wallet.setBlacklisted(true);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        WalletOperationResult result =
                walletService.debit(1L, BigDecimal.TEN, "test");

        assertEquals("UNKNOWN_ERROR",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testDebit_UserBlacklisted_ReturnsFailureUnknown() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(true);

        WalletOperationResult result =
                walletService.debit(1L, BigDecimal.TEN, "test");

        assertEquals("UNKNOWN_ERROR",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testDebit_LimitExceeded() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletConfig.getMaxDebitLimit()).thenReturn(BigDecimal.ONE);
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(false);

        WalletOperationResult result =
                walletService.debit(1L, BigDecimal.TEN, "test");

        assertEquals("LIMIT_EXCEEDED",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testDebit_InsufficientFunds() {
        wallet.setBalance(BigDecimal.valueOf(10));

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(false);
        when(walletConfig.getMaxDebitLimit()).thenReturn(BigDecimal.valueOf(5000));

        WalletOperationResult result =
                walletService.debit(1L, BigDecimal.valueOf(100), "test");

        assertEquals("INSUFFICIENT_FUNDS",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testDebit_LimitValidationThrows() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletConfig.getMaxDebitLimit()).thenReturn(BigDecimal.valueOf(5000));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(false);

        doThrow(new IllegalArgumentException("limit fail"))
                .when(transactionService).validateTransactionLimits(anyLong(), any(), any());

        WalletOperationResult result =
                walletService.debit(1L, BigDecimal.valueOf(200), "test");

        assertEquals("LIMIT_EXCEEDED",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testDebit_OptimisticLock() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletConfig.getMaxDebitLimit()).thenReturn(BigDecimal.valueOf(5000));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(false);
        doNothing().when(transactionService).validateTransactionLimits(anyLong(), any(), any());

        doThrow(new OptimisticLockException("conflict"))
                .when(walletRepository).save(any());

        WalletOperationResult result =
                walletService.debit(1L, BigDecimal.valueOf(100), "test");

        assertEquals("CONFLICT",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testDebit_Unexpected() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletConfig.getMaxDebitLimit()).thenReturn(BigDecimal.valueOf(5000));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(false);

        doNothing().when(transactionService).validateTransactionLimits(anyLong(), any(), any());
        doThrow(new RuntimeException("boom"))
                .when(transactionRepository).save(any());

        WalletOperationResult result =
                walletService.debit(1L, BigDecimal.valueOf(50), "test");

        assertEquals("UNKNOWN_ERROR",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testDebit_Success() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletConfig.getMaxDebitLimit()).thenReturn(BigDecimal.valueOf(5000));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(false);

        doNothing().when(transactionService).validateTransactionLimits(anyLong(), any(), any());
        when(walletRepository.save(any())).thenReturn(wallet);
        when(transactionRepository.save(any())).thenReturn(new Transaction());

        WalletOperationResult result =
                walletService.debit(1L, BigDecimal.valueOf(100), "test");

        assertTrue(result instanceof WalletOperationResult.Success);
    }

    // ------------------------------------------------------------
    // transfer() — REAL EXCEPTION PROPAGATION
    // ------------------------------------------------------------

    @Test
    void testTransfer_SameWalletInvalid() {
        WalletOperationResult result =
                walletService.transfer(1L, 1L, BigDecimal.TEN, "test");

        assertEquals("INVALID_TRANSFER",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testTransfer_WalletNotFound() {
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());

        WalletOperationResult result =
                walletService.transfer(1L, 2L, BigDecimal.TEN, "test");

        assertEquals("NOT_FOUND",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testTransfer_BlacklistedWallet_Throws() {
        wallet.setBlacklisted(true);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.findById(2L)).thenReturn(Optional.of(wallet));

        assertThrows(WalletBlacklistedException.class,
                () -> walletService.transfer(1L, 2L, BigDecimal.TEN, "test"));
    }

    @Test
    void testTransfer_UserBlacklisted_Throws() {
        Wallet w1 = new Wallet(1L, "w1", BigDecimal.valueOf(500), 10L);
        Wallet w2 = new Wallet(2L, "w2", BigDecimal.valueOf(500), 20L);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(w1));
        when(walletRepository.findById(2L)).thenReturn(Optional.of(w2));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(true);

        assertThrows(WalletBlacklistedException.class,
                () -> walletService.transfer(1L, 2L, BigDecimal.TEN, "test"));
    }

    @Test
    void testTransfer_InvalidAmount() {
        Wallet w1 = new Wallet(1L, "w1", BigDecimal.valueOf(500), 10L);
        Wallet w2 = new Wallet(2L, "w2", BigDecimal.valueOf(500), 20L);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(w1));
        when(walletRepository.findById(2L)).thenReturn(Optional.of(w2));

        WalletOperationResult result =
                walletService.transfer(1L, 2L, BigDecimal.ZERO, "test");

        assertEquals("INVALID_AMOUNT",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testTransfer_Success() {
        Wallet w1 = new Wallet(1L, "W1", BigDecimal.valueOf(500), 10L);
        Wallet w2 = new Wallet(2L, "W2", BigDecimal.valueOf(300), 20L);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(w1));
        when(walletRepository.findById(2L)).thenReturn(Optional.of(w2));
        when(userClient.isUserBlacklisted(anyLong())).thenReturn(false);

        doReturn(new WalletOperationResult.Success("OK"))
                .when(walletService).debit(anyLong(), any(), any());
        doReturn(new WalletOperationResult.Success("OK"))
                .when(walletService).credit(anyLong(), any(), any());

        WalletOperationResult result =
                walletService.transfer(1L, 2L, BigDecimal.valueOf(100), "test");

        assertTrue(result instanceof WalletOperationResult.Success);
    }

    // ------------------------------------------------------------
    // getBalance()
    // ------------------------------------------------------------

    @Test
    void testGetBalance_Success() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        WalletOperationResult result =
                walletService.getBalance(1L);

        assertTrue(result instanceof WalletOperationResult.Balance);
    }

    @Test
    void testGetBalance_NotFound() {
        when(walletRepository.findById(99L)).thenReturn(Optional.empty());

        WalletOperationResult result =
                walletService.getBalance(99L);

        assertEquals("WALLET_NOT_FOUND",
                ((WalletOperationResult.Failure) result).errorCode());
    }

    @Test
    void testGetBalance_Unexpected() {
        when(walletRepository.findById(1L)).thenThrow(new RuntimeException("boom"));

        WalletOperationResult result =
                walletService.getBalance(1L);

        assertEquals("UNKNOWN_ERROR",
                ((WalletOperationResult.Failure) result).errorCode());
    }
}
