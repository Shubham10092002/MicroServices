package com.example.wallet_service.controller.walletController;

import com.example.wallet_service.client.userClient.UserClient;
import com.example.wallet_service.data.WalletOperationResult;
import com.example.wallet_service.dto.walletDto.CreateWalletDTO;
import com.example.wallet_service.model.wallet.Wallet;
import com.example.wallet_service.security.UserPrincipal;
import com.example.wallet_service.service.walletService.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private WalletController walletController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    private void mockAuth(Long userId, String username, String role) {
        UserPrincipal principal = new UserPrincipal(userId, username, role);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ===================== CREATE WALLET (SELF) =====================

    @Test
    void createWalletForSelf_shouldCreateWalletForCurrentUser() {
        mockAuth(10L, "shivam", "USER");

        CreateWalletDTO dto = new CreateWalletDTO("My Wallet", BigDecimal.valueOf(500));

        Wallet savedWallet = new Wallet(1L, "My Wallet", BigDecimal.valueOf(500), 10L);
        when(walletService.createWalletForUser(10L, dto)).thenReturn(savedWallet);

        ResponseEntity<?> response = walletController.createWalletForSelf(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Wallet created successfully", body.get("message"));
        assertEquals(1L, body.get("walletId"));
        assertEquals("My Wallet", body.get("walletName"));
        assertEquals(BigDecimal.valueOf(500), body.get("balance"));

        verify(walletService, times(1)).createWalletForUser(10L, dto);
    }

    // ===================== GET BALANCE =====================

    @Test
    void getWalletBalance_shouldReturn404WhenWalletNotFound() {
        mockAuth(10L, "shivam", "USER");

        when(walletService.getWalletById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = walletController.getWalletBalance(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("WALLET_NOT_FOUND", body.get("errorCode"));
    }

    @Test
    void getWalletBalance_shouldReturnBalanceForOwner() {
        mockAuth(10L, "shivam", "USER");

        Wallet wallet = new Wallet(1L, "Main", BigDecimal.valueOf(1000), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));
        when(walletService.getBalance(1L))
                .thenReturn(new WalletOperationResult.Balance(1L, "1000.00"));

        ResponseEntity<?> response = walletController.getWalletBalance(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(1L, body.get("walletId"));
        assertEquals("1000.00", body.get("balance"));
    }

    @Test
    void getWalletBalance_shouldAllowAdminToViewAnyWallet() {
        mockAuth(99L, "admin", "ADMIN");

        Wallet wallet = new Wallet(1L, "Main", BigDecimal.valueOf(1000), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));
        when(walletService.getBalance(1L))
                .thenReturn(new WalletOperationResult.Balance(1L, "1000.00"));

        ResponseEntity<?> response = walletController.getWalletBalance(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getWalletBalance_shouldForbidNonOwnerNonAdmin() {
        mockAuth(20L, "randomUser", "USER");

        Wallet wallet = new Wallet(1L, "Main", BigDecimal.valueOf(1000), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));

        ResponseEntity<?> response = walletController.getWalletBalance(1L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ACCESS_DENIED", body.get("errorCode"));

        verify(walletService, never()).getBalance(1L);
    }

    @Test
    void getWalletBalance_shouldReturnBadRequestWhenServiceFails() {
        mockAuth(10L, "shivam", "USER");

        Wallet wallet = new Wallet(1L, "Main", BigDecimal.valueOf(1000), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));
        when(walletService.getBalance(1L))
                .thenReturn(new WalletOperationResult.Failure("SOME_ERROR", "Something went wrong"));

        ResponseEntity<?> response = walletController.getWalletBalance(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("SOME_ERROR", body.get("errorCode"));
    }

    // ===================== TRANSACTION SUMMARY =====================

    @Test
    void getTransactionSummary_shouldReturn404IfWalletNotFound() {
        mockAuth(10L, "shivam", "USER");

        when(walletService.getWalletById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = walletController.getTransactionSummary(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getTransactionSummary_shouldReturnSummaryForOwner() {
        mockAuth(10L, "shivam", "USER");

        Wallet wallet = new Wallet(1L, "Main", BigDecimal.valueOf(500), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));
        when(walletService.getTransactionSummary(1L)).thenReturn(List.of());

        ResponseEntity<?> response = walletController.getTransactionSummary(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(walletService, times(1)).getTransactionSummary(1L);
    }

    @Test
    void getTransactionSummary_shouldForbidNonOwnerNonAdmin() {
        mockAuth(20L, "otherUser", "USER");

        Wallet wallet = new Wallet(1L, "Main", BigDecimal.valueOf(500), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));

        ResponseEntity<?> response = walletController.getTransactionSummary(1L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(walletService, never()).getTransactionSummary(anyLong());
    }

    // ===================== CREDIT =====================

    @Test
    void creditWallet_shouldReturn404WhenWalletNotFound() {
        mockAuth(10L, "shivam", "USER");

        when(walletService.getWalletById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response =
                walletController.creditWallet(1L, BigDecimal.valueOf(100), "test");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void creditWallet_shouldForbidNonOwnerNonAdmin() {
        mockAuth(20L, "user", "USER");

        Wallet wallet = new Wallet(1L, "Main", BigDecimal.valueOf(500), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));

        ResponseEntity<?> response =
                walletController.creditWallet(1L, BigDecimal.valueOf(100), "test");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(walletService, never()).credit(anyLong(), any(), any());
    }

    @Test
    void creditWallet_shouldRejectNonPositiveAmount() {
        mockAuth(10L, "shivam", "USER");

        Wallet wallet = new Wallet(1L, "Main", BigDecimal.valueOf(500), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));

        ResponseEntity<?> response =
                walletController.creditWallet(1L, BigDecimal.ZERO, "test");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("INVALID_AMOUNT", body.get("errorCode"));
    }

    @Test
    void creditWallet_shouldReturnSuccessWhenOperationSucceeds() {
        mockAuth(10L, "shivam", "USER");

        Wallet wallet = new Wallet(1L, "Main", BigDecimal.valueOf(500), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));
        when(walletService.credit(1L, BigDecimal.valueOf(100), "test"))
                .thenReturn(new WalletOperationResult.Success("New Balance: 600.00"));

        ResponseEntity<?> response =
                walletController.creditWallet(1L, BigDecimal.valueOf(100), "test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("New Balance: 600.00", body.get("message"));
    }

    // ===================== DEBIT =====================

    @Test
    void debitWallet_shouldReturn404WhenWalletNotFound() {
        mockAuth(10L, "shivam", "USER");

        when(walletService.getWalletById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response =
                walletController.debitWallet(1L, BigDecimal.valueOf(100), "test");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void debitWallet_shouldForbidNonOwnerNonAdmin() {
        mockAuth(20L, "user", "USER");

        Wallet wallet = new Wallet(1L, "Main", BigDecimal.valueOf(500), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));

        ResponseEntity<?> response =
                walletController.debitWallet(1L, BigDecimal.valueOf(100), "test");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(walletService, never()).debit(anyLong(), any(), any());
    }

    @Test
    void debitWallet_shouldRejectNonPositiveAmount() {
        mockAuth(10L, "shivam", "USER");

        Wallet wallet = new Wallet(1L, "Main", BigDecimal.valueOf(500), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));

        ResponseEntity<?> response =
                walletController.debitWallet(1L, BigDecimal.ZERO, "test");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("INVALID_AMOUNT", body.get("errorCode"));
    }

    @Test
    void debitWallet_shouldReturnSuccessWhenOperationSucceeds() {
        mockAuth(10L, "shivam", "USER");

        Wallet wallet = new Wallet(1L, "Main", BigDecimal.valueOf(500), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));
        when(walletService.debit(1L, BigDecimal.valueOf(100), "test"))
                .thenReturn(new WalletOperationResult.Success("New Balance: 400.00"));

        ResponseEntity<?> response =
                walletController.debitWallet(1L, BigDecimal.valueOf(100), "test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("New Balance: 400.00", body.get("message"));
    }

    // ===================== TRANSFER =====================

    @Test
    void transfer_shouldReturn404WhenSourceWalletNotFound() {
        mockAuth(10L, "shivam", "USER");

        when(walletService.getWalletById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response =
                walletController.transfer(1L, 2L, BigDecimal.valueOf(100), "test");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("NOT_FOUND", body.get("errorCode"));
    }

    @Test
    void transfer_shouldForbidNonOwnerNonAdmin() {
        mockAuth(20L, "user", "USER");

        Wallet fromWallet = new Wallet(1L, "From", BigDecimal.valueOf(500), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(fromWallet));

        ResponseEntity<?> response =
                walletController.transfer(1L, 2L, BigDecimal.valueOf(100), "test");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(walletService, never()).transfer(anyLong(), anyLong(), any(), any());
    }

    @Test
    void transfer_shouldReturnSuccessWhenOperationSucceeds() {
        mockAuth(10L, "shivam", "USER");

        Wallet fromWallet = new Wallet(1L, "From", BigDecimal.valueOf(500), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(fromWallet));
        when(walletService.transfer(1L, 2L, BigDecimal.valueOf(100), "test"))
                .thenReturn(new WalletOperationResult.Success("Transfer successful"));

        ResponseEntity<?> response =
                walletController.transfer(1L, 2L, BigDecimal.valueOf(100), "test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Transfer successful", body.get("message"));
    }

    @Test
    void transfer_shouldReturnBadRequestWhenServiceFails() {
        mockAuth(10L, "shivam", "USER");

        Wallet fromWallet = new Wallet(1L, "From", BigDecimal.valueOf(500), 10L);
        when(walletService.getWalletById(1L)).thenReturn(Optional.of(fromWallet));
        when(walletService.transfer(1L, 2L, BigDecimal.valueOf(100), "test"))
                .thenReturn(new WalletOperationResult.Failure("INVALID_AMOUNT", "Amount must be greater than zero"));

        ResponseEntity<?> response =
                walletController.transfer(1L, 2L, BigDecimal.valueOf(100), "test");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("INVALID_AMOUNT", body.get("errorCode"));
    }
}
