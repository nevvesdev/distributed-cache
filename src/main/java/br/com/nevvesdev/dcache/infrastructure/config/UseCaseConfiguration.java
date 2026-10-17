package br.com.nevvesdev.dcache.infrastructure.config;

import br.com.nevvesdev.dcache.application.port.in.EvictAccountCacheUseCase;
import br.com.nevvesdev.dcache.application.port.in.GetAccountSummaryUseCase;
import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import br.com.nevvesdev.dcache.application.port.out.AccountRepository;
import br.com.nevvesdev.dcache.application.service.EvictAccountCacheService;
import br.com.nevvesdev.dcache.application.service.GetAccountSummaryService;
import br.com.nevvesdev.dcache.application.service.SingleFlightAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UseCaseConfiguration {

    @Bean
    GetAccountSummaryUseCase getAccountSummaryUseCase(AccountRepository accountRepository, AccountCache accountCache) {
        return new GetAccountSummaryService(new SingleFlightAccountRepository(accountRepository), accountCache);
    }

    @Bean
    EvictAccountCacheUseCase evictAccountCacheUseCase(AccountCache accountCache) {
        return new EvictAccountCacheService(accountCache);
    }
}
