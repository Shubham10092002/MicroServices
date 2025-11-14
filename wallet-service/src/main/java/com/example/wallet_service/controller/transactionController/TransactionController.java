package com.example.wallet_service.controller.transactionController;

import com.example.wallet_service.dto.transactionDto.TransactionDTO;
import com.example.wallet_service.model.transaction.Transaction;
import com.example.wallet_service.model.wallet.Wallet;
import com.example.wallet_service.security.UserPrincipal;
import com.example.wallet_service.service.transactionService.TransactionService;
import com.example.wallet_service.service.walletService.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    private final WalletService walletService;

    public TransactionController(TransactionService transactionService, WalletService walletService) {
        this.transactionService = transactionService;
        this.walletService = walletService;
    }

    //  Helper to get current authenticated user
    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    //  Helper for 403 Forbidden response
    private ResponseEntity<?> forbidden(String message) {
        return ResponseEntity.status(403).body(Map.of(
                "errorCode", "ACCESS_DENIED",
                "reason", message
        ));
    }

    // Helper for 404 Not Found response
    private ResponseEntity<?> notFound(String message) {
        return ResponseEntity.status(404).body(Map.of(
                "errorCode", "NOT_FOUND",
                "reason", message
        ));
    }


    // ======================== GET TRANSACTIONS BY WALLET ID ========================
    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<?> getTransactionsByWallet(@PathVariable Long walletId) {
        UserPrincipal principal = getCurrentUser();

        Wallet wallet = walletService.getWalletById(walletId).orElse(null);
        if (wallet == null) {
            return notFound("Wallet not found with ID " + walletId);
        }

        //  Ownership check
        if (!"ADMIN".equalsIgnoreCase(principal.getRole()) &&
                !wallet.getUserId().equals(principal.getUserId())) {
            return forbidden("You can only view transactions of your own wallets");
        }

        return ResponseEntity.ok(transactionService.getTransactionsByWallet(walletId));
    }

    // ======================== GET TRANSACTION HISTORY (ADMIN ONLY) ========================
    @GetMapping("/history")
    public ResponseEntity<?> getTransactionHistory(
            @RequestParam(required = false) Long walletId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Wallet wallet = walletService.getWalletById(walletId).orElse(null);

        if (wallet == null) {
            return notFound("Wallet not found with ID " + walletId);
        }

        UserPrincipal principal = getCurrentUser();

        if (!"ADMIN".equalsIgnoreCase(principal.getRole()) &&
                !wallet.getUserId().equals(principal.getUserId())) {
            return forbidden("You can only view transactions of your own wallets");
        }

        try {

            var transactionPage = transactionService.getTransactionHistory(walletId, type, start, end, page, size);

            //  Return only the list (no pagination metadata)
            return ResponseEntity.ok(transactionPage.getContent());


        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "INVALID_REQUEST",
                    "reason", e.getMessage()
            ));
        }
    }

    // ======================== GET USER TRANSACTIONS (USER or ADMIN) ========================

    @GetMapping("/usertransactions")
    public ResponseEntity<?> getUserTransactions(
           // @PathVariable Long userId,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) String type
    ) {
        UserPrincipal principal = getCurrentUser();
        Long userId = principal.getUserId();

        //  Only the same user or ADMIN can access
        if (!"ADMIN".equalsIgnoreCase(principal.getRole()) &&
                !userId.equals(principal.getUserId())) {
            return forbidden("You can only view your own transactions");
        }

        Object response = transactionService.getUserTransactions(userId, start, end, type);
        if (response instanceof String) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "INVALID_INPUT",
                    "reason", response
            ));
        }

        return ResponseEntity.ok(response);
    }

    //------------------------- GET TRANSACTION BY TRANSACTION_ID ------------------------
@GetMapping("/{transactionId}")
public ResponseEntity<?> getTransactionById(@PathVariable Long transactionId) {
    UserPrincipal principal = getCurrentUser();

    Optional<Transaction> transactionOpt = transactionService.getTransactionById(transactionId);

    //  Handle not found
    if (transactionOpt.isEmpty()) {
        return ResponseEntity.status(404).body(Map.of(
                "errorCode", "NOT_FOUND",
                "reason", "Transaction not found with ID: " + transactionId
        ));
    }

    Transaction transaction = transactionOpt.get();
    Long walletOwnerId = transaction.getWallet().getUserId();

    //  Authorization: allow only ADMIN or the wallet owner
    if (!"ADMIN".equalsIgnoreCase(principal.getRole()) &&
            !walletOwnerId.equals(principal.getUserId())) {

        return ResponseEntity.status(403).body(Map.of(
                "errorCode", "ACCESS_DENIED",
                "reason", "You can only view your own transactions"
        ));
    }

    //  Return clean DTO, not entity
    TransactionDTO transactionDTO = new TransactionDTO(transaction);
    return ResponseEntity.ok(transactionDTO);
}


}
