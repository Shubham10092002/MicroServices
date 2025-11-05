package com.example.wallet_service.service;

import com.example.wallet_service.config.WalletConfig;
import com.example.wallet_service.data.WalletOperationResult;
import com.example.wallet_service.dto.TransactionSummaryDTO;
import com.example.wallet_service.dto.WalletBalanceDTO;
import com.example.wallet_service.model.Transaction;
import com.example.wallet_service.model.Wallet;
import com.example.wallet_service.repository.TransactionRepository;
import com.example.wallet_service.repository.WalletRepository;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletConfig walletConfig;
    private final TransactionService transactionService;

    public WalletService(WalletRepository walletRepository,
                         TransactionRepository transactionRepository,
                         WalletConfig walletConfig,
                         TransactionService transactionService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.walletConfig = walletConfig;
        this.transactionService = transactionService;
    }



    /**
     * Creates a default wallet for a given user (called from user-service)
     */
    public Wallet createDefaultWalletForUser(Long userId) {
        Wallet wallet = new Wallet();
        wallet.setWalletName("Default Wallet");
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUserId(userId); // since we now store userId instead of a User entity

        return walletRepository.save(wallet);
    }

//    public List<Wallet> findWalletsWithBalanceGreaterThan(BigDecimal threshold) {
//        return walletRepository.findByBalanceGreaterThan(threshold);
//    }

    public List<WalletBalanceDTO> findWalletsWithBalanceGreaterThan(BigDecimal threshold) {
        List<Wallet> wallets = walletRepository.findByBalanceGreaterThan(threshold);

        return wallets.stream()
                .map(wallet -> new WalletBalanceDTO(
                        wallet.getId(),
                        wallet.getWalletName(),
                        wallet.getBalance()
                ))
                .toList();
    }






    // ========================= BASIC READ METHODS =========================

    public List<Wallet> getAllWallets() {
        logger.info("Fetching all wallets");
        List<Wallet> wallets = walletRepository.findAll();
        logger.debug("Total wallets fetched: {}", wallets.size());
        return wallets;
    }

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

    public List<WalletBalanceDTO> getWalletBalancesByUser(Long userId) {
        logger.info("Fetching wallet balances for user ID: {}", userId);
        List<WalletBalanceDTO> balances = walletRepository.getWalletBalancesByUserId(userId);
        logger.debug("Fetched {} wallet balances for user {}", balances.size(), userId);
        return balances;
    }

    public BigDecimal getTotalBalanceByUser(Long userId) {
        logger.info("Calculating total balance for user ID: {}", userId);
        BigDecimal totalBalance = walletRepository.getTotalBalanceByUserId(userId);
        if (totalBalance == null) {
            logger.warn("Total balance is null for user {}, setting to ZERO", userId);
            totalBalance = BigDecimal.ZERO;
        }
        logger.debug("Total balance for user {} is {}", userId, totalBalance);
        return totalBalance;
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
