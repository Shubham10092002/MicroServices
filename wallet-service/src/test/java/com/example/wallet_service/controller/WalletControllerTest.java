package com.example.wallet_service.controller;

import com.example.wallet_service.client.UserClient;
import com.example.wallet_service.data.WalletOperationResult;
import com.example.wallet_service.dto.CreateWalletDTO;
import com.example.wallet_service.dto.TransactionSummaryDTO;
import com.example.wallet_service.dto.WalletBalanceDTO;
import com.example.wallet_service.exception.GlobalExceptionHandler;
import com.example.wallet_service.model.Wallet;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.security.UserPrincipal;
import com.example.wallet_service.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("removal")

class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @MockBean
    private WalletRepository walletRepository;

    @MockBean
    private UserClient userClient;

    @Autowired
    private ObjectMapper objectMapper;

    private UserPrincipal userPrincipal;
    private UserPrincipal adminPrincipal;
    private Wallet wallet;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        userPrincipal = new UserPrincipal(1L, "user1", "USER");
        adminPrincipal = new UserPrincipal(2L, "admin", "ADMIN");

        wallet = new Wallet();
        wallet.setId(1L);
        wallet.setWalletName("Main Wallet");
        wallet.setBalance(BigDecimal.valueOf(1000));
        wallet.setUserId(1L);
    }

    // Helper to simulate logged-in user
    private void setAuthentication(UserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    // ============================================================
    // ✅ GET ALL WALLETS TESTS
    // ============================================================
    @Nested
    @DisplayName("Get All Wallets Tests")
    class GetAllWalletsTests {

        @Test
        @DisplayName("✅ should allow ADMIN to get all wallets")
        void shouldAllowAdminToGetAllWallets() throws Exception {
            setAuthentication(adminPrincipal);
            when(walletService.getAllWallets()).thenReturn(List.of(wallet));

            mockMvc.perform(get("/api/wallets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].walletName").value("Main Wallet"));
        }

        @Test
        @DisplayName("❌ should deny USER from accessing all wallets")
        void shouldDenyUserFromGettingAllWallets() throws Exception {
            setAuthentication(userPrincipal);

            mockMvc.perform(get("/api/wallets"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }

        @Test
        @DisplayName("❌ should fail when walletName is missing or blank")
        void shouldFailWhenWalletNameMissing() throws Exception {
            setAuthentication(userPrincipal);
            CreateWalletDTO dto = new CreateWalletDTO("", BigDecimal.valueOf(500));

            mockMvc.perform(post("/api/wallets/user/1/create-wallet")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

    }

    // ============================================================
    // ✅ CREATE WALLET TESTS
    // ============================================================
    @Nested
    @DisplayName("Create Wallet Tests")
    class CreateWalletTests {

        @Test
        @DisplayName("✅ should allow user to create their own wallet")
        void shouldAllowUserToCreateOwnWallet() throws Exception {
            setAuthentication(userPrincipal);
            CreateWalletDTO dto = new CreateWalletDTO("Savings", BigDecimal.valueOf(500));

            when(walletRepository.saveAndFlush(any(Wallet.class))).thenReturn(wallet);

            mockMvc.perform(post("/api/wallets/user/1/create-wallet")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.walletName").value("Main Wallet"));
        }

        @Test
        @DisplayName("❌ should deny user from creating wallet for another user")
        void shouldDenyUserFromCreatingOtherUserWallet() throws Exception {
            setAuthentication(userPrincipal);
            CreateWalletDTO dto = new CreateWalletDTO("Other Wallet", BigDecimal.valueOf(500));

            mockMvc.perform(post("/api/wallets/user/2/create-wallet")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }
    }

    // ============================================================
    // ✅ GET WALLETS BY USER
    // ============================================================
    @Nested
    @DisplayName("Get Wallets By User Tests")
    class GetWalletsByUserTests {

        @Test
        @DisplayName("✅ should return wallet balances for own user")
        void shouldReturnWalletBalancesForOwnUser() throws Exception {
            setAuthentication(userPrincipal);
            when(walletService.getWalletBalancesByUser(1L))
                    .thenReturn(List.of(new WalletBalanceDTO(1L, "Main Wallet", BigDecimal.valueOf(1000))));

            mockMvc.perform(get("/api/wallets/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].walletName").value("Main Wallet"));
        }

        @Test
        @DisplayName("❌ should return 404 when no wallets found")
        void shouldReturn404WhenNoWalletsFound() throws Exception {
            setAuthentication(userPrincipal);
            when(walletService.getWalletBalancesByUser(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/wallets/user/1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
        }
    }

    // ============================================================
    // ✅ CREDIT & DEBIT TESTS
    // ============================================================
    @Nested
    @DisplayName("Credit & Debit Tests")
    class CreditDebitTests {

        @Test
        @DisplayName("✅ should credit wallet successfully")
        void shouldCreditWalletSuccessfully() throws Exception {
            setAuthentication(userPrincipal);
            when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));
            when(walletService.credit(anyLong(), any(), any()))
                    .thenReturn(new WalletOperationResult.Success("New Balance: 1500.00"));

            mockMvc.perform(post("/api/wallets/1/credit")
                            .param("amount", "500"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("New Balance: 1500.00"));
        }

        @Test
        @DisplayName("❌ should deny credit for non-owner")
        void shouldDenyCreditForNonOwner() throws Exception {
            // simulate a regular user (not admin) trying to access someone else's wallet
            UserPrincipal anotherUser = new UserPrincipal(3L, "otherUser", "USER");
            setAuthentication(anotherUser);

            wallet.setUserId(999L); // wallet belongs to someone else
            when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));

            mockMvc.perform(post("/api/wallets/1/credit")
                            .param("amount", "500"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                    .andExpect(jsonPath("$.reason").value("You can only credit your own wallet"));
        }


        @Test
        @DisplayName("✅ should debit wallet successfully")
        void shouldDebitWalletSuccessfully() throws Exception {
            setAuthentication(userPrincipal);
            when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));
            when(walletService.debit(anyLong(), any(), any()))
                    .thenReturn(new WalletOperationResult.Success("New Balance: 500.00"));

            mockMvc.perform(post("/api/wallets/1/debit")
                            .param("amount", "500"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("New Balance: 500.00"));
        }
    }

    // ============================================================
    // ✅ TRANSFER TESTS
    // ============================================================
    @Nested
    @DisplayName("Transfer Tests")
    class TransferTests {

        @Test
        @DisplayName("✅ should transfer successfully between wallets")
        void shouldTransferSuccessfully() throws Exception {
            setAuthentication(userPrincipal);
            Wallet fromWallet = new Wallet(1L, "Source", BigDecimal.valueOf(1000), 1L);
            Wallet toWallet = new Wallet(2L, "Target", BigDecimal.valueOf(500), 2L);

            when(walletService.getWalletById(1L)).thenReturn(Optional.of(fromWallet));
            when(walletService.getWalletById(2L)).thenReturn(Optional.of(toWallet));
            when(walletService.debit(eq(1L), any(), any()))
                    .thenReturn(new WalletOperationResult.Success("Debited"));
            when(walletService.credit(eq(2L), any(), any()))
                    .thenReturn(new WalletOperationResult.Success("Credited"));

            mockMvc.perform(post("/api/wallets/transfer")
                            .param("fromWalletId", "1")
                            .param("toWalletId", "2")
                            .param("amount", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Amount transferred successfully"));
        }


        @Test
        @DisplayName("❌ should fail when transferring to same wallet")
        void shouldFailWhenTransferringToSameWallet() throws Exception {
            setAuthentication(userPrincipal);
            when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));

            mockMvc.perform(post("/api/wallets/transfer")
                            .param("fromWalletId", "1")
                            .param("toWalletId", "1")
                            .param("amount", "100"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSFER"));
        }
    }

    // ============================================================
// ✅ TOTAL BALANCE TESTS
// ============================================================
    @Nested
    @DisplayName("Total Balance Tests")
    class TotalBalanceTests {

        @Test
        @DisplayName("✅ should return total balance for own user")
        void shouldReturnTotalBalanceForOwnUser() throws Exception {
            setAuthentication(userPrincipal);
            when(walletService.getTotalBalanceByUser(1L)).thenReturn(BigDecimal.valueOf(1500));

            mockMvc.perform(get("/api/wallets/user/1/totalBalance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalBalance").value(1500.00));
        }

        @Test
        @DisplayName("❌ should deny access to other user’s total balance")
        void shouldDenyAccessToOtherUserTotalBalance() throws Exception {
            setAuthentication(userPrincipal);

            mockMvc.perform(get("/api/wallets/user/2/totalBalance"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }
    }

    // ============================================================
// ✅ TRANSACTION SUMMARY TESTS
// ============================================================
    @Nested
    @DisplayName("Transaction Summary Tests")
    class TransactionSummaryTests {

        @Test
        @DisplayName("✅ should return transaction summary for own wallet")
        void shouldReturnTransactionSummaryForOwnWallet() throws Exception {
            setAuthentication(userPrincipal);
            when(walletService.getWalletById(1L)).thenReturn(Optional.of(wallet));
            when(walletService.getTransactionSummary(1L))
                    .thenReturn(List.of(new TransactionSummaryDTO(
                            com.example.wallet_service.model.Transaction.Type.CREDIT,
                            BigDecimal.valueOf(500))));

            mockMvc.perform(get("/api/wallets/1/transactions/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].type").value("CREDIT"));
        }

        @Test
        @DisplayName("❌ should return 404 if wallet not found")
        void shouldReturn404IfWalletNotFound() throws Exception {
            setAuthentication(userPrincipal);
            when(walletService.getWalletById(1L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/wallets/1/transactions/summary"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
        }
    }


//    @Test
//    @DisplayName("❌ should fail when walletName is missing or blank")
//    void shouldFailWhenWalletNameMissing() throws Exception {
//        setAuthentication(userPrincipal);
//        CreateWalletDTO dto = new CreateWalletDTO("", BigDecimal.valueOf(500));
//
//        mockMvc.perform(post("/api/wallets/user/1/create-wallet")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(dto)))
//                .andExpect(status().isBadRequest());
//    }

}
