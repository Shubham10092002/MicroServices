package com.example.wallet_service.controller;

import com.example.wallet_service.client.UserClient;
import com.example.wallet_service.data.WalletOperationResult;
import com.example.wallet_service.dto.CreateWalletDTO;
import com.example.wallet_service.dto.TransactionSummaryDTO;
import com.example.wallet_service.dto.UserDTO;
import com.example.wallet_service.dto.WalletBalanceDTO;
import com.example.wallet_service.exception.WalletBlacklistedException;
import com.example.wallet_service.model.Wallet;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.security.UserPrincipal;
import com.example.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;
    //private final WalletRepository walletRepository;
    private final UserClient userClient;

    public WalletController(WalletService walletService, UserClient userClient) {
        this.walletService = walletService;
        //this.walletRepository = walletRepository;
        this.userClient = userClient;
    }

    // ========================== GET ALL WALLETS (Admin only) ==========================
//    @GetMapping
//    public ResponseEntity<?> getAllWallets() {
//        UserPrincipal principal = getCurrentUser();
//        if (!"ADMIN".equalsIgnoreCase(principal.getRole())) {
//            return ResponseEntity.status(403).body(Map.of(
//                    "errorCode", "ACCESS_DENIED",
//                    "reason", "Only ADMINs can view all wallets"
//            ));
//        }
//        return ResponseEntity.ok(walletService.getAllWallets());
//    }

    // ========================== CREATE WALLET (Only Self) ==========================
    @PostMapping("/user/{userId}/create-wallet")
    public ResponseEntity<?> createWalletForUser(
            @PathVariable Long userId,
            @Valid @RequestBody CreateWalletDTO walletDTO
    ) {
        UserPrincipal principal = getCurrentUser();

        // 🧩 Only the logged-in user or admin can create a wallet
        if (!"ADMIN".equalsIgnoreCase(principal.getRole()) && !userId.equals(principal.getUserId())) {
            return ResponseEntity.status(403).body(Map.of(
                    "errorCode", "ACCESS_DENIED",
                    "reason", "You can only create wallets for your own account"
            ));
        }

        Wallet wallet = walletService.createWalletForUser(userId, walletDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    // ========================== GET WALLETS BY USER ==========================

//    @GetMapping("/user/{userId}")
//    public ResponseEntity<?> getWalletBalancesByUser(@PathVariable Long userId) {
//        UserPrincipal principal = getCurrentUser();
//
//        if (!"ADMIN".equalsIgnoreCase(principal.getRole()) && !userId.equals(principal.getUserId())) {
//            return ResponseEntity.status(403).body(Map.of(
//                    "errorCode", "ACCESS_DENIED",
//                    "reason", "You can only view your own wallets"
//            ));
//        }
//
//        List<WalletBalanceDTO> balances = walletService.getWalletBalancesByUser(userId);
//        if (balances.isEmpty()) {
//            return ResponseEntity.status(404).body(Map.of(
//                    "errorCode", "NOT_FOUND",
//                    "reason", "No wallets found for user " + userId
//            ));
//        }
//        return ResponseEntity.ok(balances);
//    }



    // ========================== GET BALANCE ==========================

    @GetMapping("/{walletId}")
    public ResponseEntity<?> getWalletBalance(@PathVariable Long walletId) {
        UserPrincipal principal = getCurrentUser();

        // Fetch wallet from service
        Optional<Wallet> walletOpt = walletService.getWalletById(walletId);
        if (walletOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("errorCode", "WALLET_NOT_FOUND", "message", "Wallet not found with ID " + walletId));
        }

        Wallet wallet = walletOpt.get();

        //  Authorization check — user can only see their own wallet
        boolean isAdmin = "ADMIN".equalsIgnoreCase(principal.getRole());
        boolean ownsWallet = wallet.getUserId().equals(principal.getUserId());

        if (!isAdmin && !ownsWallet) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("errorCode", "ACCESS_DENIED", "message", "You can only view your own wallet balance"));
        }

        //  Delegate to service for actual balance retrieval
        WalletOperationResult result = walletService.getBalance(walletId);

        //  Build proper response
        if (result instanceof WalletOperationResult.Balance balance) {
            return ResponseEntity.ok(Map.of(
                    "walletId", balance.walletId(),
                    "balance", balance.balance()
            ));
        }

        if (result instanceof WalletOperationResult.Failure failure) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("errorCode", failure.errorCode(), "message", failure.reason()));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("errorCode", "UNKNOWN_ERROR", "message", "Unexpected error"));
    }


//    @GetMapping("/user/{userId}/totalBalance")
//    public ResponseEntity<?> getTotalBalanceByUser(@PathVariable Long userId) {
//        UserPrincipal principal = getCurrentUser();
//
//        if (!"ADMIN".equalsIgnoreCase(principal.getRole()) && !userId.equals(principal.getUserId())) {
//            return ResponseEntity.status(403).body(Map.of(
//                    "errorCode", "ACCESS_DENIED",
//                    "reason", "You can only view your own balance"
//            ));
//        }
//
//        BigDecimal totalBalance = walletService.getTotalBalanceByUser(userId);
//        return ResponseEntity.ok(Map.of("userId", userId, "totalBalance", totalBalance.setScale(2)));
//    }


    // ========================== GET TRANSACTION SUMMARY ==========================
    @GetMapping("/{walletId}/transactions/summary")
    public ResponseEntity<?> getTransactionSummary(@PathVariable Long walletId) {
        UserPrincipal principal = getCurrentUser();

        Wallet wallet = walletService.getWalletById(walletId).orElse(null);
        if (wallet == null) {
            return ResponseEntity.status(404).body(Map.of("errorCode", "NOT_FOUND", "reason", "Wallet not found"));
        }

        // 🧩 Ownership check
        if (!"ADMIN".equalsIgnoreCase(principal.getRole()) && !wallet.getUserId().equals(principal.getUserId())) {
            return ResponseEntity.status(403).body(Map.of(
                    "errorCode", "ACCESS_DENIED",
                    "reason", "You can only view your own wallet transactions"
            ));
        }

        return ResponseEntity.ok(walletService.getTransactionSummary(walletId));
    }

    // ========================== CREDIT ==========================
    @PostMapping("/{walletId}/credit")
    public ResponseEntity<?> creditWallet(@PathVariable Long walletId, @RequestParam BigDecimal amount,
                                          @RequestParam(required = false) String description) {

        UserPrincipal principal = getCurrentUser();
        Wallet wallet = walletService.getWalletById(walletId).orElse(null);

        if (wallet == null)
            return ResponseEntity.status(404).body(Map.of("errorCode", "NOT_FOUND", "reason", "Wallet not found"));

        if (!"ADMIN".equalsIgnoreCase(principal.getRole()) && !wallet.getUserId().equals(principal.getUserId()))
            return forbidden("You can only credit your own wallet");

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            return badRequest("INVALID_AMOUNT", "Amount must be positive");

        return buildResponse(walletService.credit(walletId, amount, description));
    }

    // ========================== DEBIT ==========================
    @PostMapping("/{walletId}/debit")
    public ResponseEntity<?> debitWallet(@PathVariable Long walletId, @RequestParam BigDecimal amount,
                                         @RequestParam(required = false) String description) {

        UserPrincipal principal = getCurrentUser();
        Wallet wallet = walletService.getWalletById(walletId).orElse(null);

        if (wallet == null)
            return ResponseEntity.status(404).body(Map.of("errorCode", "NOT_FOUND", "reason", "Wallet not found"));

        if (!"ADMIN".equalsIgnoreCase(principal.getRole()) && !wallet.getUserId().equals(principal.getUserId()))
            return forbidden("You can only debit your own wallet");

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            return badRequest("INVALID_AMOUNT", "Amount must be positive");

        return buildResponse(walletService.debit(walletId, amount, description));
    }



    // ========================== TRANSFER ==========================




    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestParam Long fromWalletId,
                                      @RequestParam Long toWalletId,
                                      @RequestParam BigDecimal amount,
                                      @RequestParam(required = false) String description) {

        UserPrincipal principal = getCurrentUser();
        Wallet fromWallet = walletService.getWalletById(fromWalletId).orElse(null);

        if (fromWallet == null)
            return ResponseEntity.status(404).body(Map.of("errorCode", "NOT_FOUND", "reason", "Source wallet not found"));

        // Ownership check
        boolean isAdmin = "ADMIN".equalsIgnoreCase(principal.getRole());
        boolean ownsFromWallet = fromWallet.getUserId().equals(principal.getUserId());
        if (!isAdmin && !ownsFromWallet)
            return forbidden("You can only transfer from your own wallet");

        // Delegate to service
        WalletOperationResult result = walletService.transfer(fromWalletId, toWalletId, amount, description);
        return buildResponse(result);
    }




//    @PostMapping("/transfer")
//    public ResponseEntity<?> transfer(@RequestParam Long fromWalletId,
//                                      @RequestParam Long toWalletId,
//                                      @RequestParam BigDecimal amount,
//                                      @RequestParam(required = false) String description) {
//
//        UserPrincipal principal = getCurrentUser();
//
//        Wallet fromWallet = walletService.getWalletById(fromWalletId).orElse(null);
//        Wallet toWallet = walletService.getWalletById(toWalletId).orElse(null);
//
//        if (fromWallet == null || toWallet == null)
//            return ResponseEntity.status(404).body(Map.of("errorCode", "NOT_FOUND", "reason", "One or both wallets not found"));
//
//        //  Check blacklisted wallets
//        if (fromWallet.isBlacklisted() || toWallet.isBlacklisted()) {
//            throw new WalletBlacklistedException("This wallet has been blacklisted. Transactions are not allowed.");
//        }
//
//
////        // 🔹 2. Check if the wallet's owner (user) is blacklisted
////        UserDTO user = userClient.getUserById(wallet.getUserId());
////        if (user != null && user.isBlacklisted()) {
////            throw new WalletBlacklistedException(
////                    "The user owning this wallet is blacklisted. Transactions are not allowed."
////            );
////        }
//
//
//
//        //  Ownership or Admin privilege check
//        boolean isAdmin = "ADMIN".equalsIgnoreCase(principal.getRole());
//        boolean ownsFromWallet = fromWallet.getUserId().equals(principal.getUserId());
//
//        if (!isAdmin && !ownsFromWallet) {
//            return forbidden("You can only transfer from your own wallet");
//        }
//
//        //  Prevent self-transfers
//        if (fromWalletId.equals(toWalletId))
//            return badRequest("INVALID_TRANSFER", "Cannot transfer to the same wallet");
//
//        if (amount.compareTo(BigDecimal.ZERO) <= 0)
//            return badRequest("INVALID_AMOUNT", "Amount must be positive");
//
//        //  Perform transaction
//        WalletOperationResult debitResult = walletService.debit(fromWalletId, amount, description);
//        if (debitResult instanceof WalletOperationResult.Failure failure)
//            return badRequest(failure.errorCode(), failure.reason());
//
//        walletService.credit(toWalletId, amount, description);
//
//        return ResponseEntity.ok(Map.of("message", "Amount transferred successfully"));
//    }


    // ========================== HELPER METHODS ==========================
    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private ResponseEntity<?> badRequest(String code, String reason) {
        return ResponseEntity.badRequest().body(Map.of("errorCode", code, "reason", reason));
    }

    private ResponseEntity<?> forbidden(String reason) {
        return ResponseEntity.status(403).body(Map.of("errorCode", "ACCESS_DENIED", "reason", reason));
    }

    private ResponseEntity<?> buildResponse(WalletOperationResult result) {
        if (result instanceof WalletOperationResult.Success success)
            return ResponseEntity.ok(Map.of("message", success.message()));
        if (result instanceof WalletOperationResult.Balance balance)
            return ResponseEntity.ok(Map.of("walletId", balance.walletId(), "balance", balance.balance()));
        if (result instanceof WalletOperationResult.Failure failure)
            return badRequest(failure.errorCode(), failure.reason());
        return ResponseEntity.internalServerError().body(Map.of("errorCode", "UNKNOWN_ERROR"));
    }
}
