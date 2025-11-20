package com.example.wallet_service.controller.admin;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletAdminControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletAdminController walletAdminController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext(); // IMPORTANT FIX
    }

    private void mockAuth(Long id, String username, String role) {
        UserPrincipal principal = new UserPrincipal(id, username, role);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ------------------------------------------------------------
    // NON-ADMIN BLACKLIST FAILS
    // ------------------------------------------------------------
    @Test
    void testToggleBlacklist_NonAdminForbidden() {

        mockAuth(2L, "user", "USER");

        // Updated URL mapping:
        // PATCH /api/wallets/admin/{id}/blacklist
        ResponseEntity<?> response = walletAdminController.toggleBlacklist(10L, true);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ACCESS_DENIED", body.get("errorCode"));

        verify(walletService, never()).toggleBlacklistStatus(anyLong(), anyBoolean());
    }

    // ------------------------------------------------------------
    // ADMIN BLACKLIST SUCCESS
    // ------------------------------------------------------------
    @Test
    void testToggleBlacklist_AdminSuccess() {

        mockAuth(1L, "admin", "ADMIN");

        Wallet wallet = new Wallet();
        wallet.setId(10L);
        wallet.setBlacklisted(true);

        when(walletService.toggleBlacklistStatus(10L, true)).thenReturn(wallet);

        ResponseEntity<?> response = walletAdminController.toggleBlacklist(10L, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(true, body.get("blacklisted"));
        assertEquals(10L, body.get("walletId"));
    }

    // ------------------------------------------------------------
    // ADMIN CREATE WALLET SUCCESS
    // ------------------------------------------------------------
    @Test
    void testCreateWallet_AdminSuccess() {

        mockAuth(1L, "admin", "ADMIN");

        CreateWalletDTO dto = new CreateWalletDTO();
        dto.setWalletName("WalletX");

        Wallet wallet = new Wallet();
        wallet.setId(50L);
        wallet.setWalletName("WalletX");
        wallet.setBalance(BigDecimal.ZERO);

        // Updated method signature:
        // createWalletForUserAsAdmin(@RequestParam Long userId, @RequestBody CreateWalletDTO dto)
        when(walletService.createWalletForUser(5L, dto)).thenReturn(wallet);

        ResponseEntity<?> response =
                walletAdminController.createWalletForUserAsAdmin(5L, dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("WalletX", body.get("walletName"));
    }

    // ------------------------------------------------------------
    // NON-ADMIN CREATION FAILS
    // ------------------------------------------------------------
    @Test
    void testCreateWallet_NonAdminForbidden() {

        mockAuth(3L, "user", "USER");

        CreateWalletDTO dto = new CreateWalletDTO();
        dto.setWalletName("TestWallet");

        // New method: createWalletForUserAsAdmin(userId, dto)
        ResponseEntity<?> response =
                walletAdminController.createWalletForUserAsAdmin(5L, dto);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("ACCESS_DENIED", body.get("errorCode"));

        verify(walletService, never()).createWalletForUser(anyLong(), any());
    }
}
