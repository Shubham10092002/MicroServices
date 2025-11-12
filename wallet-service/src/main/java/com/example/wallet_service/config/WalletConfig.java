package com.example.wallet_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
public class WalletConfig {

//    @Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate();
//    }


    @Value("${wallet.max-credit-limit}")
    private BigDecimal maxCreditLimit;

    @Value("${wallet.max-debit-limit}")
    private BigDecimal maxDebitLimit;

    @Value("${wallet.daily-debit-limit}")
    private BigDecimal dailyDebitLimit;

    @Value("${wallet.monthly-debit-limit}")
    private BigDecimal monthlyDebitLimit;

    @Value("${wallet.daily-credit-limit}")
    private BigDecimal dailyCreditLimit;

    @Value("${wallet.monthly-credit-limit}")
    private BigDecimal monthlyCreditLimit;

    public BigDecimal getMaxCreditLimit() { return maxCreditLimit; }
    public BigDecimal getMaxDebitLimit() { return maxDebitLimit; }

    public BigDecimal getDailyDebitLimit() { return dailyDebitLimit; }
    public BigDecimal getMonthlyDebitLimit() { return monthlyDebitLimit; }
    public BigDecimal getDailyCreditLimit() { return dailyCreditLimit; }
    public BigDecimal getMonthlyCreditLimit() { return monthlyCreditLimit; }
}
