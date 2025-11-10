package com.example.wallet_service.controller;

import com.example.wallet_service.dto.TransactionDTO;
import com.example.wallet_service.model.Transaction;
import com.example.wallet_service.model.Wallet;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.security.UserPrincipal;
import com.example.wallet_service.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("removal")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private WalletRepository walletRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UserPrincipal adminPrincipal;
    private UserPrincipal userPrincipal;
    private Wallet wallet;
    private TransactionDTO transactionDTO;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        adminPrincipal = new UserPrincipal(1L, "admin", "ADMIN");
        userPrincipal = new UserPrincipal(2L, "user", "USER");

        wallet = new Wallet();
        wallet.setId(1L);
        wallet.setWalletName("Main Wallet");
        wallet.setBalance(BigDecimal.valueOf(1000));
        wallet.setUserId(2L);

        transactionDTO = new TransactionDTO();
        transactionDTO.setAmount(BigDecimal.valueOf(500));
        transactionDTO.setType("CREDIT");
    }

    // helper to simulate authentication
    private void setAuthentication(UserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    // ============================================================
    // ✅ GET TRANSACTION HISTORY TESTS
    // ============================================================
    @Nested
    @DisplayName("Get Transaction History Tests")
    class TransactionHistoryTests {

        @Test
        @DisplayName("✅ should allow ADMIN to fetch transaction history")
        void shouldAllowAdminToFetchHistory() throws Exception {
            setAuthentication(adminPrincipal);

            // ✅ Prepare a Page<TransactionDTO> as the mock return
            Page<TransactionDTO> mockPage = new PageImpl<>(
                    List.of(transactionDTO),
                    PageRequest.of(0, 10),
                    1
            );

            when(transactionService.getTransactionHistory(
                    any(), any(), any(), any(), any(), anyInt(), anyInt()
            )).thenReturn(mockPage);

            mockMvc.perform(get("/api/transactions/history")
                            .param("userId", "2")
                            .param("walletId", "1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].amount").value(500))
                    .andExpect(jsonPath("$.content[0].type").value("CREDIT"));
        }

        @Test
        @DisplayName("❌ should forbid USER from fetching transaction history")
        void shouldForbidUserFromFetchingHistory() throws Exception {
            setAuthentication(userPrincipal);

            mockMvc.perform(get("/api/transactions/history"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                    .andExpect(jsonPath("$.reason").value("Only ADMIN can access transaction history"));
        }

        @Test
        @DisplayName("❌ should return 400 for invalid date format")
        void shouldReturn400ForInvalidRequest() throws Exception {
            setAuthentication(adminPrincipal);

            when(transactionService.getTransactionHistory(
                    any(), any(), any(), any(), any(), anyInt(), anyInt()
            )).thenThrow(new IllegalArgumentException("Invalid date format. Please use dd-MM-yyyy."));

            mockMvc.perform(get("/api/transactions/history")
                            .param("start", "bad-date"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.reason").value("Invalid date format. Please use dd-MM-yyyy."));
        }
    }

    // ============================================================
    // ✅ GET TRANSACTIONS BY WALLET TESTS
    // ============================================================
    @Nested
    @DisplayName("Get Transactions By Wallet Tests")
    class GetTransactionsByWalletTests {

        @Test
        @DisplayName("✅ should allow wallet owner to view their transactions")
        void shouldAllowWalletOwnerToViewTransactions() throws Exception {
            setAuthentication(userPrincipal);

            when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
            when(transactionService.getTransactionsByWallet(1L)).thenReturn(List.of(transactionDTO));

            mockMvc.perform(get("/api/transactions/wallet/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].amount").value(500))
                    .andExpect(jsonPath("$[0].type").value("CREDIT"));
        }

        @Test
        @DisplayName("❌ should forbid user from viewing another user's wallet transactions")
        void shouldForbidViewingOthersWalletTransactions() throws Exception {
            setAuthentication(new UserPrincipal(99L, "otherUser", "USER"));
            when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

            mockMvc.perform(get("/api/transactions/wallet/1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                    .andExpect(jsonPath("$.reason").value("You can only view transactions of your own wallets"));
        }

        @Test
        @DisplayName("❌ should return 404 if wallet not found")
        void shouldReturn404IfWalletNotFound() throws Exception {
            setAuthentication(adminPrincipal);
            when(walletRepository.findById(1L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/transactions/wallet/1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.reason").value("Wallet not found with ID 1"));
        }
    }

    // ============================================================
    // ✅ GET ALL TRANSACTIONS TESTS
    // ============================================================
    @Nested
    @DisplayName("Get All Transactions Tests")
    class GetAllTransactionsTests {

        @Test
        @DisplayName("✅ should allow ADMIN to view all transactions")
        void shouldAllowAdminToViewAllTransactions() throws Exception {
            setAuthentication(adminPrincipal);
            when(transactionService.getAllTransactions()).thenReturn(List.of(transactionDTO));

            mockMvc.perform(get("/api/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].amount").value(500));
        }

        @Test
        @DisplayName("❌ should forbid USER from viewing all transactions")
        void shouldForbidUserFromViewingAllTransactions() throws Exception {
            setAuthentication(userPrincipal);

            mockMvc.perform(get("/api/transactions"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                    .andExpect(jsonPath("$.reason").value("Only ADMIN can view all transactions"));
        }
    }
}
