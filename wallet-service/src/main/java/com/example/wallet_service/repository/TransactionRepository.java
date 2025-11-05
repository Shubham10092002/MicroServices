package com.example.wallet_service.repository;

import com.example.wallet_service.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("""
    SELECT t FROM Transaction t
    JOIN t.wallet w
    WHERE (:walletId IS NULL OR w.id = :walletId)
    AND (:userId IS NULL OR w.userId = :userId)
    AND (:type IS NULL OR t.type = :type)
    AND (:startDate IS NULL OR t.timestamp >= :startDate)
    AND (:endDate IS NULL OR t.timestamp <= :endDate)
    ORDER BY t.timestamp DESC
    """)
    Page<Transaction> findTransactionsWithFilters(
            @Param("walletId") Long walletId,
            @Param("userId") Long userId,
            @Param("type") Transaction.Type type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM Transaction t
        JOIN t.wallet w
        WHERE w.userId = :userId
        AND t.timestamp BETWEEN :startDate AND :endDate
        AND (:type IS NULL OR t.type = :type)
        ORDER BY t.timestamp DESC
        """)
    List<Transaction> findUserTransactionsBetweenDates(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("type") Transaction.Type type
    );

    @Query("""
    SELECT COALESCE(SUM(t.amount), 0)
    FROM Transaction t
    WHERE t.wallet.id = :walletId
    AND t.type = :type
    AND t.timestamp BETWEEN :start AND :end
    """)
    BigDecimal getTotalAmountByWalletAndTypeBetweenDates(
            @Param("walletId") Long walletId,
            @Param("type") Transaction.Type type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<Transaction> findByWalletId(Long walletId);

    @Query("SELECT t.type, SUM(t.amount) FROM Transaction t WHERE t.wallet.id = :walletId GROUP BY t.type")
    List<Object[]> getTransactionSumsByType(@Param("walletId") Long walletId);

    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId ORDER BY t.timestamp DESC")
    List<Transaction> findTopTransactionsByWalletId(@Param("walletId") Long walletId);
}
