package com.example.user_service.client;

import com.example.user_service.dto.CreateWalletDTO;
import com.example.user_service.dto.Wallet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class WalletClient {

    private final RestClient restClient;

    @Value("${wallet.service.url}")
    private String walletServiceUrl;

    public WalletClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    // ✅ Call wallet-service to get wallets above threshold
    public List<Wallet> getWalletsWithBalanceGreaterThan(BigDecimal threshold) {
        String url = walletServiceUrl + "/api/wallets/balance-greater/" + threshold;

        Wallet[] wallets = restClient.get()
                .uri(url)
                .retrieve()
                .body(Wallet[].class);

        return wallets != null ? Arrays.asList(wallets) : List.of();
    }

    // ✅ Call wallet-service to create default wallet for user
//    public Wallet createDefaultWalletForUser(Long userId, String username, String jwtToken) {
//        CreateWalletDTO walletDTO = new CreateWalletDTO();
//        walletDTO.setWalletName("Default Wallet");
//        walletDTO.setInitialBalance(BigDecimal.ZERO);
//
//        var request = restClient.post()
//                .uri(walletServiceUrl + "/api/wallets/user/{userId}/create-wallet", userId)
//                .body(walletDTO);
//
//        // ✅ Add Authorization header only if token exists
//        if (jwtToken != null && !jwtToken.isEmpty()) {
//            request.header("Authorization", "Bearer " + jwtToken);
//        }
//
//        return request.retrieve().body(Wallet.class);
//    }


}
