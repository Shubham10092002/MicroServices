package com.example.wallet_service.repository.walletRepository;

import com.example.wallet_service.dto.walletDto.WalletBalanceDTO;
import com.example.wallet_service.model.wallet.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    //List<Wallet> findByUserId(Long userId);
    //List<Wallet> findByBalanceGreaterThan(BigDecimal threshold);
    Optional<Wallet> findByIdAndBlacklistedFalse(Long id);

    //  Corrected query - use w.userId
//    @Query("SELECT SUM(w.balance) FROM Wallet w WHERE w.userId = :userId")
//    BigDecimal getTotalBalanceByUserId(@Param("userId") Long userId);

    // ✅ Corrected DTO mapping query - use your current DTO package path
//    @Query("SELECT new com.example.wallet_service.dto.walletDto.WalletBalanceDTO(w.id, w.walletName, w.balance) " +
//            "FROM Wallet w WHERE w.userId = :userId")
//    List<WalletBalanceDTO> getWalletBalancesByUserId(@Param("userId") Long userId);

}

// indexing o