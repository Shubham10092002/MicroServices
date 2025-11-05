package com.example.wallet_service.controller;

import com.example.wallet_service.client.UserClient;
import com.example.wallet_service.data.WalletOperationResult;
import com.example.wallet_service.dto.CreateWalletDTO;
import com.example.wallet_service.dto.TransactionSummaryDTO;
import com.example.wallet_service.dto.UserDTO;
import com.example.wallet_service.dto.WalletBalanceDTO;
import com.example.wallet_service.exception.UserNotFoundException;

import com.example.wallet_service.model.Wallet;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;
    private WalletRepository walletRepository;
    private final UserClient userClient;

    public WalletController(WalletService walletService, WalletRepository walletRepository, UserClient userClient ) {
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.userClient = userClient;

    }






//@Transactional(readOnly = true)

    // ---------- GET ALL WALLETS ----------
    @GetMapping
    public ResponseEntity<?> getAllWallets() {
        try {
            var wallets = walletService.getAllWallets();
            return ResponseEntity.ok(wallets);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("errorCode", "UNKNOWN_ERROR",
                            "reason", "Failed to fetch wallets: " + e.getMessage())
            );
        }
    }



    // @Transactional
    //---------CREATE NEW WALLET------------
//    @PostMapping("/user/{userId}/create-wallet")
//    public ResponseEntity<?> createWalletForUser(
//            @PathVariable Long userId,
//            @Valid @RequestBody CreateWalletDTO walletDTO
//    ) {
//        // 1️⃣ Fetch user from User Service
////        UserDTO user = userClient.getUserById(userId);
////        if (user == null) {
////            throw new UserNotFoundException(userId);
////        }
//
//        // 2️⃣ Create Wallet
//        Wallet wallet = new Wallet();
//        wallet.setWalletName(walletDTO.getWalletName());
//        wallet.setBalance(walletDTO.getInitialBalance());
//        wallet.setUserId(userId); // ✅ replaced user object with userId
//
//        walletRepository.save(wallet);
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
//    }


@PostMapping("/user/{userId}/create-wallet")
public ResponseEntity<?> createWalletForUser(
        @PathVariable Long userId,
        @Valid @RequestBody CreateWalletDTO walletDTO
) {
    Wallet wallet = new Wallet();
    wallet.setWalletName(walletDTO.getWalletName());
    wallet.setBalance(walletDTO.getInitialBalance());
    wallet.setUserId(userId);

    Wallet savedWallet = walletRepository.saveAndFlush(wallet); // 👈 ensures ID is generated

    return ResponseEntity.status(HttpStatus.CREATED).body(savedWallet);
}




    // @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)

    // ---------- GET WALLETS BY USER ----------
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getWalletBalancesByUser(@PathVariable Long userId) {
        try {
            List<WalletBalanceDTO> balances = walletService.getWalletBalancesByUser(userId);
            if (balances.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "errorCode", "NOT_FOUND",
                        "reason", "No wallets found for user ID " + userId
                ));
            }
            return ResponseEntity.ok(balances);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "errorCode", "UNKNOWN_ERROR",
                    "reason", "Failed to fetch balances: " + e.getMessage()
            ));
        }
    }


//    @GetMapping("/balance-greater/{threshold}")
//    public ResponseEntity<List<Wallet>> getWalletsWithBalanceGreaterThan(@PathVariable BigDecimal threshold) {
//        List<Wallet> wallets = walletService.findWalletsWithBalanceGreaterThan(threshold);
//        return ResponseEntity.ok(wallets);
//    }


    @GetMapping("/balance-greater/{threshold}")
    public ResponseEntity<List<WalletBalanceDTO>> getWalletsWithBalanceGreaterThan(@PathVariable BigDecimal threshold) {
        List<WalletBalanceDTO> walletDTOs = walletService.findWalletsWithBalanceGreaterThan(threshold);
        return ResponseEntity.ok(walletDTOs);
    }



    // @Transactional(readOnly = true)
    // ---------- GET TOTAL BALANCE BY USER ----------
    @GetMapping("/user/{userId}/totalBalance")
    public ResponseEntity<?> getTotalBalanceByUser(@PathVariable Long userId) {
        BigDecimal totalBalance = walletService.getTotalBalanceByUser(userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "totalBalance", totalBalance.setScale(2)
        ));
    }


    // @Transactional(readOnly = true)
    // ---------- GET TRANSACTION SUMMARY BY WALLET ----------
    @GetMapping("/{walletId}/transactions/summary")
    public ResponseEntity<List<TransactionSummaryDTO>> getTransactionSummary(@PathVariable Long walletId) {
        List<TransactionSummaryDTO> summary = walletService.getTransactionSummary(walletId);
        return ResponseEntity.ok(summary);
    }

    // ---------- CREDIT ----------
    @PostMapping("/{walletId}/credit")
    public ResponseEntity<?> creditWallet(
            @PathVariable Long walletId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "INVALID_AMOUNT",
                    "reason", "Amount must be positive"
            ));
        }

        WalletOperationResult result = walletService.credit(walletId, amount, description);
        return buildResponse(result);
    }

    // ---------- DEBIT ----------
    @PostMapping("/{walletId}/debit")
    public ResponseEntity<?> debitWallet(
            @PathVariable Long walletId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "INVALID_AMOUNT",
                    "reason", "Amount must be positive"
            ));
        }

        WalletOperationResult result = walletService.debit(walletId, amount, description);
        return buildResponse(result);
    }



    // ---------- GET BALANCE ----------
    //  @Transactional(readOnly = true)
    @GetMapping("/{walletId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable Long walletId) {
        WalletOperationResult result = walletService.getBalance(walletId);
        return buildResponse(result);
    }








    // ---------- TRANSFER ----------
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(
            @RequestParam Long fromWalletId,
            @RequestParam Long toWalletId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {

        if (fromWalletId.equals(toWalletId)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "INVALID_TRANSFER",
                    "reason", "Cannot transfer to the same wallet"
            ));
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "INVALID_AMOUNT",
                    "reason", "Amount must be positive"
            ));
        }

        WalletOperationResult debitResult = walletService.debit(fromWalletId, amount, description);
        if (debitResult instanceof WalletOperationResult.Failure failure) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", failure.errorCode(),
                    "reason", failure.reason()
            ));
        }

        walletService.credit(toWalletId, amount, description);

        return ResponseEntity.ok(Map.of("message", "Amount transferred successfully"));
    }

    // ---------- HELPER METHOD ----------
    private ResponseEntity<?> buildResponse(WalletOperationResult result) {
        if (result instanceof WalletOperationResult.Success success) {
            return ResponseEntity.ok(Map.of("message", success.message()));
        } else if (result instanceof WalletOperationResult.Balance balance) {
            return ResponseEntity.ok(Map.of(
                    "walletId", balance.walletId(),
                    "balance", balance.balance(),
                    "message", "Balance retrieved successfully"
            ));
        } else if (result instanceof WalletOperationResult.Failure failure) {
            return ResponseEntity.ok(Map.of(
                    "errorCode", failure.errorCode(),
                    "reason", failure.reason()
            ));
        } else {
            return ResponseEntity.internalServerError().body(Map.of(
                    "errorCode", "UNKNOWN_ERROR",
                    "reason", "Unexpected result type"
            ));
        }
    }
}
