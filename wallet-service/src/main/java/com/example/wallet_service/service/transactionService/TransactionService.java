package com.example.wallet_service.service.transactionService;

import com.example.wallet_service.config.walletConfig.WalletConfig;
import com.example.wallet_service.dto.transactionDto.TransactionDTO;
import com.example.wallet_service.exception.WalletIdNotFoundException;
import com.example.wallet_service.model.transaction.Transaction;
import com.example.wallet_service.model.wallet.Wallet;
import com.example.wallet_service.repository.transactionRepository.TransactionRepository;
import com.example.wallet_service.repository.walletRepository.WalletRepository;
import com.example.wallet_service.service.walletService.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;

    private final WalletConfig walletConfig;
    private final WalletRepository walletRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              WalletConfig walletConfig, WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;

        this.walletConfig = walletConfig;
        this.walletRepository = walletRepository;

    }


    /**
     * Get all transactions for a specific wallet.
     */

    public List<TransactionDTO> getTransactionsByWallet(Long walletId) {
        logger.info("Fetching transactions for wallet ID {}", walletId);

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletIdNotFoundException("Wallet ID not found: " + walletId));

        //wallet.ifPresent(w -> {})
//        if (!wallet.isPresent()) {
//            throw new WalletIdNotFoundException("Wallet ID not found: " + walletId);
//        }

        return transactionRepository.findByWalletId(walletId)
                .stream()
                .map(TransactionDTO::new)
                .collect(Collectors.toList());
    }


    // ----------------- GET TRANSACTION BY TRANSACTION ID ----------

    public Optional<Transaction> getTransactionById(Long transactionId) {
        logger.info("Fetching transaction by ID: {}", transactionId);

        Optional<Transaction> transaction = transactionRepository.findById(transactionId);

        if (transaction.isPresent()) {
            logger.debug("Transaction found: {}", transaction.get());
        } else {
            logger.warn("Transaction not found with ID: {}", transactionId);
        }

        return transaction;
    }




    /**
     * Get transactions for a specific user between two dates, optionally filtered by type.
     */

    public Object getUserTransactions(Long userId, String start, String end, String type) {
        logger.info("Fetching transactions for user {} from {} to {} with type {}", userId, start, end, type);

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDateTime startDate = LocalDate.parse(start, formatter).atStartOfDay();
            LocalDateTime endDate = LocalDate.parse(end, formatter).atTime(23, 59, 59);

            Transaction.Type transactionType = null;
            if (type != null) {
                transactionType = Transaction.Type.valueOf(type.toUpperCase());
            }

            return transactionRepository.findUserTransactionsBetweenDates(userId, startDate, endDate, transactionType)
                    .stream()
                    .map(TransactionDTO::new)
                    .collect(Collectors.toList());

        } catch (DateTimeParseException e) {
            logger.error("Invalid date format provided: start={}, end={}", start, end, e);
            return "Invalid date format. Please use dd-MM-yyyy.";
        } catch (IllegalArgumentException e) {
            logger.error("Invalid transaction type provided: {}", type, e);
            return "Invalid transaction type. Use CREDIT, DEBIT, or TRANSFER.";
        }
    }


    public Page<TransactionDTO> getTransactionHistory(
            Long walletId,
            String type,
            String start,
            String end,
            int page,
            int size
    ) {
        logger.info("Fetching transaction history:  walletId={}, type={}, start={}, end={}, page={}, size={}",
                 walletId, type, start, end, page, size);

        Transaction.Type transactionType = null;
        if (type != null && !type.isBlank()) {
            try {
                transactionType = Transaction.Type.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid transaction type: " + type);
            }
        }

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        try {
            if (start != null && !start.isBlank()) {
                startDate = LocalDate.parse(start, formatter).atStartOfDay();
            }
            if (end != null && !end.isBlank()) {
                endDate = LocalDate.parse(end, formatter).atTime(23, 59, 59);
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Please use dd-MM-yyyy.");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Transaction> transactionPage = transactionRepository.findTransactionsWithFilters(
                walletId,
                transactionType,
                startDate,
                endDate,
                pageable
        );

        return transactionPage.map(TransactionDTO::new);
    }





    // ========================= NEW FUNCTIONALITY =========================

    /**
     * Validate transaction limits before saving
     */


    public void validateTransactionLimits(Long walletId, BigDecimal amount, Transaction.Type type) {
        ZoneId appZone = ZoneId.of("Asia/Kolkata");
        ZoneOffset dbZone = ZoneOffset.UTC;

        // Get current time in IST
        LocalDate todayIST = LocalDate.now(appZone);

        // Compute IST day/month boundaries
        LocalDateTime startOfDayIST = todayIST.atStartOfDay();
        LocalDateTime endOfDayIST = startOfDayIST.plusDays(1).minusNanos(1);
        LocalDateTime startOfMonthIST = todayIST.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonthIST = startOfMonthIST.plusMonths(1).minusNanos(1);

        // Convert to UTC before querying DB
        LocalDateTime startOfDayUTC = startOfDayIST.atZone(appZone).withZoneSameInstant(dbZone).toLocalDateTime();
        LocalDateTime endOfDayUTC = endOfDayIST.atZone(appZone).withZoneSameInstant(dbZone).toLocalDateTime();
        LocalDateTime startOfMonthUTC = startOfMonthIST.atZone(appZone).withZoneSameInstant(dbZone).toLocalDateTime();
        LocalDateTime endOfMonthUTC = endOfMonthIST.atZone(appZone).withZoneSameInstant(dbZone).toLocalDateTime();

        logger.debug("Checking limits for wallet {} [{}]: {} - {}", walletId, type, startOfDayUTC, endOfDayUTC);

        BigDecimal dailyTotal = transactionRepository
                .getTotalAmountByWalletAndTypeBetweenDates(walletId, type, startOfDayUTC, endOfDayUTC);
        BigDecimal monthlyTotal = transactionRepository
                .getTotalAmountByWalletAndTypeBetweenDates(walletId, type, startOfMonthUTC, endOfMonthUTC);

        if (dailyTotal == null) dailyTotal = BigDecimal.ZERO;
        if (monthlyTotal == null) monthlyTotal = BigDecimal.ZERO;

        BigDecimal dailyLimit, monthlyLimit;
        if (type == Transaction.Type.DEBIT) {
            dailyLimit = walletConfig.getDailyDebitLimit();
            monthlyLimit = walletConfig.getMonthlyDebitLimit();
        } else if (type == Transaction.Type.CREDIT) {
            dailyLimit = walletConfig.getDailyCreditLimit();
            monthlyLimit = walletConfig.getMonthlyCreditLimit();
        } else {
            throw new IllegalArgumentException("Unsupported transaction type: " + type);
        }

        if (dailyTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new IllegalArgumentException(String.format(
                    "%s daily limit exceeded: Attempt %.2f > Allowed %.2f",
                    type, dailyTotal.add(amount), dailyLimit));
        }

        if (monthlyTotal.add(amount).compareTo(monthlyLimit) > 0) {
            throw new IllegalArgumentException(String.format(
                    "%s monthly limit exceeded: Attempt %.2f > Allowed %.2f",
                    type, monthlyTotal.add(amount), monthlyLimit));
        }

        logger.info("{} transaction within limits. Daily total: {}, Monthly total: {}",
                type, dailyTotal, monthlyTotal);
    }



    /**
     * Example method to create a transaction with validation.
     */
    public TransactionDTO createTransaction(Long walletId, BigDecimal amount, Transaction.Type type) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletIdNotFoundException("Wallet ID not found: " + walletId));

        validateTransactionLimits(walletId, amount, type);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

        Transaction saved = transactionRepository.save(transaction);
        return new TransactionDTO(saved);
    }

}
