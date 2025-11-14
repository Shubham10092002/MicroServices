package com.example.wallet_service.service.walletService;

import com.example.wallet_service.client.userClient.UserClient;
import com.example.wallet_service.config.walletConfig.WalletConfig;
import com.example.wallet_service.data.WalletOperationResult;
import com.example.wallet_service.dto.walletDto.CreateWalletDTO;
import com.example.wallet_service.dto.transactionDto.TransactionSummaryDTO;
import com.example.wallet_service.exception.WalletBlacklistedException;
import com.example.wallet_service.exception.WalletIdNotFoundException;
import com.example.wallet_service.model.transaction.Transaction;
import com.example.wallet_service.model.wallet.Wallet;
import com.example.wallet_service.repository.transactionRepository.TransactionRepository;
import com.example.wallet_service.repository.walletRepository.WalletRepository;
import com.example.wallet_service.security.UserPrincipal;
import com.example.wallet_service.service.transactionService.TransactionService;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletConfig walletConfig;
    private final TransactionService transactionService;
    private final UserClient userClient;

    public WalletService(WalletRepository walletRepository,
                         TransactionRepository transactionRepository,
                         WalletConfig walletConfig,
                         TransactionService transactionService, UserClient userClient) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.walletConfig = walletConfig;
        this.transactionService = transactionService;
        this.userClient = userClient;
    }



    /**
     * Creates a default wallet for a given user (called from user-service)
     */
    public Wallet createWalletForUser(Long userId, CreateWalletDTO walletDTO) {
        Wallet wallet = new Wallet();
        wallet.setWalletName(walletDTO.getWalletName());
        wallet.setBalance(walletDTO.getInitialBalance());
        wallet.setUserId(userId); // since we now store userId instead of a User entity

        return walletRepository.save(wallet);
    }




    // ========================= BASIC READ METHODS =========================

    public Optional<Wallet> getWalletById(Long id) {
        logger.info("Fetching wallet by ID: {}", id);
        Optional<Wallet> wallet = walletRepository.findById(id);
        if (wallet.isPresent()) {
            logger.debug("Wallet found: {}", wallet.get());
        } else {
            logger.warn("Wallet not found with ID: {}", id);
        }
        return wallet;
    }



    public List<TransactionSummaryDTO> getTransactionSummary(Long walletId) {
        logger.info("Fetching transaction summary for wallet ID: {}", walletId);
        List<Object[]> results = transactionRepository.getTransactionSumsByType(walletId);
        List<TransactionSummaryDTO> summary = results.stream()
                .map(r -> new TransactionSummaryDTO((Transaction.Type) r[0], (BigDecimal) r[1]))
                .collect(Collectors.toList());
        logger.debug("Transaction summary for wallet {}: {}", walletId, summary);
        return summary;
    }

    // ========================= TRANSACTION OPERATIONS =========================

    // @Transactional(noRollbackFor = IllegalArgumentException.class)
    @Transactional
    public WalletOperationResult credit(Long walletId, BigDecimal amount, String description) {
        logger.info("Credit request - walletId: {}, amount: {}, description: {}", walletId, amount, description);
        try {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Invalid credit amount: {}", amount);
                return new WalletOperationResult.Failure("INVALID_AMOUNT", "Amount must be greater than zero.");
            }


            Optional<Wallet> walletOpt = walletRepository.findById(walletId);
            if (walletOpt.isEmpty()) {
                logger.warn("Wallet not found: {}", walletId);
                return new WalletOperationResult.Failure("WALLET_NOT_FOUND", "Wallet ID " + walletId + " not found.");
            }



            Wallet wallet = walletOpt.get();

            //check the blacklist status
            if (wallet.isBlacklisted()) {
                throw new WalletBlacklistedException("This wallet has been blacklisted. Transactions are not allowed.");
            }

            Long userId = wallet.getUserId();

            verifyUserNotBlacklisted(userId);

           // UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


            if (amount.compareTo(walletConfig.getMaxCreditLimit()) > 0) {
                logger.warn("Credit amount {} exceeds max limit {}", amount, walletConfig.getMaxCreditLimit());
                return new WalletOperationResult.Failure("LIMIT_EXCEEDED", "Amount exceeds max credit limit.");
            }

            transactionService.validateTransactionLimits(walletId, amount, Transaction.Type.CREDIT);

            wallet.setBalance(wallet.getBalance().add(amount));
            walletRepository.save(wallet);
            transactionRepository.save(new Transaction(wallet, amount, Transaction.Type.CREDIT, description));

            logger.info("Credit successful - walletId: {}, new balance: {}", walletId, wallet.getBalance());
            return new WalletOperationResult.Success("New Balance: " + wallet.getBalance().setScale(2));

        } catch (IllegalArgumentException e) {
            logger.error("Credit operation failed (Validation): {}", e.getMessage());
            return new WalletOperationResult.Failure("LIMIT_EXCEEDED", e.getMessage());
        } catch (OptimisticLockException e) {
            logger.error("Credit conflict - wallet {} updated concurrently", walletId);
            return new WalletOperationResult.Failure("CONFLICT", "Wallet was updated by another transaction. Please retry.");
        } catch (Exception e) {
            logger.error("Unexpected error during credit for wallet {}: {}", walletId, e.getMessage(), e);
            return new WalletOperationResult.Failure("UNKNOWN_ERROR", "Unexpected error: " + e.getMessage());
        }
    }


    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private ResponseEntity<?> forbidden(String reason) {
        return ResponseEntity.status(403).body(Map.of("errorCode", "ACCESS_DENIED", "reason", reason));
    }


    // @Transactional(noRollbackFor = IllegalArgumentException.class)
    @Transactional
    public WalletOperationResult debit(Long walletId, BigDecimal amount, String description) {
        logger.info("Debit request - walletId: {}, amount: {}, description: {}", walletId, amount, description);
        try {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Invalid debit amount: {}", amount);
                return new WalletOperationResult.Failure("INVALID_AMOUNT", "Amount must be greater than zero.");
            }

            Optional<Wallet> walletOpt = walletRepository.findById(walletId);
            if (walletOpt.isEmpty()) {
                logger.warn("Wallet not found: {}", walletId);
                return new WalletOperationResult.Failure("WALLET_NOT_FOUND", "Wallet ID " + walletId + " not found.");
            }

            Wallet wallet = walletOpt.get();


        // check the blackList status
            if (wallet.isBlacklisted()) {
                throw new WalletBlacklistedException("This wallet has been blacklisted. Transactions are not allowed.");
            }

            Long userId = wallet.getUserId();

            // check the blackListed user
            verifyUserNotBlacklisted(userId);


            if (amount.compareTo(walletConfig.getMaxDebitLimit()) > 0) {
                logger.warn("Debit amount {} exceeds max limit {}", amount, walletConfig.getMaxDebitLimit());
                return new WalletOperationResult.Failure("LIMIT_EXCEEDED", "Amount exceeds max debit limit.");
            }

            if (wallet.getBalance().compareTo(amount) < 0) {
                logger.warn("Insufficient balance in wallet {}: available {}, requested {}", walletId, wallet.getBalance(), amount);
                return new WalletOperationResult.Failure("INSUFFICIENT_FUNDS", "Not enough balance.");
            }

            transactionService.validateTransactionLimits(walletId, amount, Transaction.Type.DEBIT);

            wallet.setBalance(wallet.getBalance().subtract(amount));
            walletRepository.save(wallet);
            transactionRepository.save(new Transaction(wallet, amount, Transaction.Type.DEBIT, description));

            logger.info("Debit successful - walletId: {}, new balance: {}", walletId, wallet.getBalance());
            return new WalletOperationResult.Success("New Balance: " + wallet.getBalance().setScale(2));

        } catch (IllegalArgumentException e) {
            logger.error("Debit operation failed (Validation): {}", e.getMessage());
            return new WalletOperationResult.Failure("LIMIT_EXCEEDED", e.getMessage());
        } catch (OptimisticLockException e) {
            logger.error("Debit conflict - wallet {} updated concurrently", walletId);
            return new WalletOperationResult.Failure("CONFLICT", "Wallet was updated by another transaction. Please retry.");
        } catch (Exception e) {
            logger.error("Unexpected error during debit for wallet {}: {}", walletId, e.getMessage(), e);
            return new WalletOperationResult.Failure("UNKNOWN_ERROR", "Unexpected error: " + e.getMessage());
        }
    }




    @Transactional
    public WalletOperationResult transfer(Long fromWalletId, Long toWalletId, BigDecimal amount, String description) {
        logger.info("Transfer request: fromWallet={}, toWallet={}, amount={}", fromWalletId, toWalletId, amount);

        if (fromWalletId.equals(toWalletId)) {
            return new WalletOperationResult.Failure("INVALID_TRANSFER", "Cannot transfer to the same wallet.");
        }

        Optional<Wallet> fromOpt = walletRepository.findById(fromWalletId);
        Optional<Wallet> toOpt = walletRepository.findById(toWalletId);

        if (fromOpt.isEmpty() || toOpt.isEmpty()) {
            return new WalletOperationResult.Failure("NOT_FOUND", "One or both wallets not found.");
        }

        Wallet fromWallet = fromOpt.get();
        Wallet toWallet = toOpt.get();

        // Check blacklisted wallets
        if (fromWallet.isBlacklisted() || toWallet.isBlacklisted()) {
            throw new WalletBlacklistedException("This wallet has been blacklisted. Transactions are not allowed.");
        }

        // Check if users of either wallet are blacklisted
        verifyUserNotBlacklisted(fromWallet.getUserId());
        verifyUserNotBlacklisted(toWallet.getUserId());

        // Check amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new WalletOperationResult.Failure("INVALID_AMOUNT", "Amount must be greater than zero.");
        }

        // Perform debit and credit
        WalletOperationResult debitResult = debit(fromWalletId, amount, description);
        if (debitResult instanceof WalletOperationResult.Failure failure) {
            return failure;
        }

        credit(toWalletId, amount, description);
        logger.info("Transfer completed: fromWallet={} → toWallet={} amount={}", fromWalletId, toWalletId, amount);

        return new WalletOperationResult.Success("Transfer successful");
    }



    // ========================= BLACKLIST STATUS OF WALLET =========================
    public Wallet toggleBlacklistStatus(Long id, boolean status) {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new WalletIdNotFoundException("Wallet not found with ID " + id));
        wallet.setBlacklisted(status);
        walletRepository.save(wallet);

        logger.info("Wallet {} blacklisted status set to {}", wallet.getId(), status);
        return wallet;
    }





    private void verifyUserNotBlacklisted(Long userId) {
        if (userClient.isUserBlacklisted(userId)) {
            throw new WalletBlacklistedException(
                    "User " + userId + " is blacklisted. Transactions are not allowed."
            );
        }
    }



    // ========================= BALANCE CHECK =========================

    public WalletOperationResult getBalance(Long walletId) {
        logger.info("Fetching balance for wallet ID: {}", walletId);
        try {
            Optional<Wallet> walletOpt = walletRepository.findById(walletId);
            if (walletOpt.isEmpty()) {
                logger.warn("Wallet not found for balance check: {}", walletId);
                return new WalletOperationResult.Failure("WALLET_NOT_FOUND", "Wallet ID " + walletId + " not found.");
            }

            Wallet wallet = walletOpt.get();
            logger.debug("Current balance for wallet {} is {}", walletId, wallet.getBalance());
            return new WalletOperationResult.Balance(walletId, wallet.getBalance().setScale(2).toPlainString());
        } catch (Exception e) {
            logger.error("Error fetching balance for wallet {}: {}", walletId, e.getMessage(), e);
            return new WalletOperationResult.Failure("UNKNOWN_ERROR", "An unexpected error occurred: " + e.getMessage());
        }
    }
}
