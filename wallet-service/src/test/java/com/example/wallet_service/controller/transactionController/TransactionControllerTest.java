package com.example.wallet_service.controller.transactionController;

import com.example.wallet_service.dto.transactionDto.TransactionDTO;
import com.example.wallet_service.model.transaction.Transaction;
import com.example.wallet_service.model.wallet.Wallet;
import com.example.wallet_service.security.UserPrincipal;
import com.example.wallet_service.service.transactionService.TransactionService;
import com.example.wallet_service.service.walletService.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private TransactionController transactionController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    private void mockAuth(Long userId, String username, String role) {
        UserPrincipal principal = new UserPrincipal(userId, username, role);

        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ----------------------------------------------------------------------------------
    // 1) GET TRANSACTIONS BY WALLET (ADMIN or OWNER)
    // ----------------------------------------------------------------------------------

    @Test
    void testGetTransactionsByWallet_AdminSuccess() {
        mockAuth(1L, "admin", "ADMIN");

        Wallet wallet = new Wallet();
        wallet.setId(100L);
        wallet.setUserId(5L);

        when(walletService.getWalletById(100L)).thenReturn(Optional.of(wallet));
        when(transactionService.getTransactionsByWallet(100L))
                .thenReturn(List.of(new TransactionDTO()));

        ResponseEntity<?> response = transactionController.getTransactionsByWallet(100L);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testGetTransactionsByWallet_OwnerSuccess() {
        mockAuth(5L, "john", "USER");

        Wallet wallet = new Wallet();
        wallet.setId(100L);
        wallet.setUserId(5L);

        when(walletService.getWalletById(100L)).thenReturn(Optional.of(wallet));
        when(transactionService.getTransactionsByWallet(100L))
                .thenReturn(List.of(new TransactionDTO()));

        ResponseEntity<?> response = transactionController.getTransactionsByWallet(100L);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testGetTransactionsByWallet_NotOwnerForbidden() {
        mockAuth(9L, "john", "USER");

        Wallet wallet = new Wallet();
        wallet.setId(100L);
        wallet.setUserId(5L);

        when(walletService.getWalletById(100L)).thenReturn(Optional.of(wallet));

        ResponseEntity<?> response = transactionController.getTransactionsByWallet(100L);

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void testGetTransactionsByWallet_NotFound() {
        mockAuth(1L, "admin", "ADMIN");

        when(walletService.getWalletById(100L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = transactionController.getTransactionsByWallet(100L);

        assertEquals(404, response.getStatusCode().value());
    }

    // ----------------------------------------------------------------------------------
    // 2) GET TRANSACTION HISTORY (ADMIN or OWNER)
    // ----------------------------------------------------------------------------------

    @Test
    void testGetTransactionHistory_AdminSuccess() {
        mockAuth(1L, "admin", "ADMIN");

        Wallet wallet = new Wallet();
        wallet.setId(5L);
        wallet.setUserId(999L);

        when(walletService.getWalletById(5L)).thenReturn(Optional.of(wallet));

        var p = new org.springframework.data.domain.PageImpl<>(List.of(new TransactionDTO()));
        when(transactionService.getTransactionHistory(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(p);

        ResponseEntity<?> response = transactionController.getTransactionHistory(
                5L, null, null, null, 0, 10
        );

        assertEquals(200, response.getStatusCode().value());
    }


    @Test
    void testGetTransactionHistory_NotOwnerForbidden() {
        mockAuth(2L, "user", "USER");

        Wallet wallet = new Wallet();
        wallet.setId(5L);
        wallet.setUserId(999L);

        when(walletService.getWalletById(5L)).thenReturn(Optional.of(wallet));

        ResponseEntity<?> response = transactionController.getTransactionHistory(
                5L, null, null, null, 0, 10
        );

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void testGetTransactionHistory_InvalidType() {
        mockAuth(1L, "admin", "ADMIN");

        Wallet wallet = new Wallet();
        wallet.setId(5L);
        wallet.setUserId(999L);

        when(walletService.getWalletById(5L)).thenReturn(Optional.of(wallet));
        when(transactionService.getTransactionHistory(eq(5L), eq("abcd"), any(), any(), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("Invalid transaction type"));

        ResponseEntity<?> response = transactionController.getTransactionHistory(
                5L, "abcd", null, null, 0, 10
        );

        assertEquals(400, response.getStatusCode().value());
    }

    // ----------------------------------------------------------------------------------
    // 3) GET USER TRANSACTIONS  (/usertransactions)
    // ----------------------------------------------------------------------------------

    @Test
    void testGetUserTransactions_UserSuccess() {
        mockAuth(5L, "john", "USER");

        when(transactionService.getUserTransactions(eq(5L), any(), any(), any()))
                .thenReturn(List.of(new TransactionDTO()));

        ResponseEntity<?> response = transactionController.getUserTransactions(
                "01-01-2025", "31-01-2025", null
        );

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testGetUserTransactions_InvalidDate() {
        mockAuth(5L, "john", "USER");

        when(transactionService.getUserTransactions(eq(5L), any(), any(), any()))
                .thenReturn("Invalid date format");

        ResponseEntity<?> response = transactionController.getUserTransactions(
                "invalid", "date", null
        );

        assertEquals(400, response.getStatusCode().value());
    }

    // ----------------------------------------------------------------------------------
    // 4) GET TRANSACTION BY ID
    // ----------------------------------------------------------------------------------

    @Test
    void testGetTransactionById_AdminSuccess() {
        mockAuth(1L, "admin", "ADMIN");

        Transaction t = new Transaction();
        Wallet w = new Wallet();
        w.setId(10L);
        w.setUserId(5L);
        t.setId(100L);
        t.setWallet(w);
        t.setAmount(BigDecimal.TEN);
        t.setType(Transaction.Type.CREDIT);
        t.setTimestamp(LocalDateTime.now());

        when(transactionService.getTransactionById(100L)).thenReturn(Optional.of(t));

        ResponseEntity<?> response = transactionController.getTransactionById(100L);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof TransactionDTO);
    }

    @Test
    void testGetTransactionById_NotOwnerForbidden() {
        mockAuth(20L, "stranger", "USER");

        Transaction t = new Transaction();
        Wallet w = new Wallet();
        w.setId(10L);
        w.setUserId(5L);
        t.setId(100L);
        t.setWallet(w);

        when(transactionService.getTransactionById(100L)).thenReturn(Optional.of(t));

        ResponseEntity<?> response = transactionController.getTransactionById(100L);

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void testGetTransactionById_NotFound() {
        mockAuth(1L, "admin", "ADMIN");

        when(transactionService.getTransactionById(100L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = transactionController.getTransactionById(100L);

        assertEquals(404, response.getStatusCode().value());
    }
}
