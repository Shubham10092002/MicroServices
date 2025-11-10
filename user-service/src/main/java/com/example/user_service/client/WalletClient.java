package com.example.user_service.client;

import com.example.user_service.dto.CreateWalletDTO;
import com.example.user_service.dto.Wallet;
import com.example.user_service.exception.WalletServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpStatusCodeException;

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

    //  Fetch wallets with balance greater than threshold
    public List<Wallet> getWalletsWithBalanceGreaterThan(BigDecimal threshold) {
        String url = walletServiceUrl + "/api/wallets/balance-greater/" + threshold;

        try {
            Wallet[] wallets = restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                        throw new WalletServiceException(
                                "WALLET_SERVICE_ERROR",
                                "Wallet service responded with error: " + res.getStatusCode()
                        );
                    })
                    .body(Wallet[].class);

            return wallets != null ? Arrays.asList(wallets) : List.of();

        } catch (HttpStatusCodeException ex) {
            throw new WalletServiceException("WALLET_HTTP_ERROR",
                    "Wallet service error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            // Handles connection refused, timeouts, etc.
            throw new WalletServiceException("WALLET_CONNECTION_ERROR",
                    "Unable to reach wallet service. Reason: " + ex.getMessage());
        } catch (Exception ex) {
            throw new WalletServiceException("WALLET_UNKNOWN_ERROR",
                    "Unexpected wallet client error: " + ex.getMessage());
        }
    }

    //  Create default wallet for user (with full error handling)
//    public Wallet createDefaultWalletForUser(Long userId, String username, String jwtToken) {
//        String url = walletServiceUrl + "/api/wallets/user/" + userId + "/create-wallet";
//        CreateWalletDTO walletDTO = new CreateWalletDTO("Default Wallet", BigDecimal.ZERO);
//
//        try {
//            var request = restClient.post()
//                    .uri(url)
//                    .body(walletDTO);
//
//            if (jwtToken != null && !jwtToken.isEmpty()) {
//                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken);
//            }
//
//            return request.retrieve()
//                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
//                        throw new WalletServiceException(
//                                "WALLET_CREATION_FAILED",
//                                "Wallet creation failed with status: " + res.getStatusCode()
//                        );
//                    })
//                    .body(Wallet.class);
//
//        } catch (RestClientException ex) {
//            throw new WalletServiceException("WALLET_CONNECTION_ERROR",
//                    "Wallet service unreachable: " + ex.getMessage());
//        } catch (Exception ex) {
//            throw new WalletServiceException("WALLET_UNKNOWN_ERROR",
//                    "Unexpected wallet error: " + ex.getMessage());
//        }
//    }
}
