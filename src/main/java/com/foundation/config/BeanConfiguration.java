package com.foundation.config;

import com.foundation.repository.AccountRepository;
import com.foundation.repository.InMemoryAccountRepository;
import com.foundation.service.TransactionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    AccountRepository accountRepository() {
        return new InMemoryAccountRepository();
    }

    @Bean
    TransactionService transactionService(AccountRepository accountRepository) {
        return new TransactionService(accountRepository);
    }
}
