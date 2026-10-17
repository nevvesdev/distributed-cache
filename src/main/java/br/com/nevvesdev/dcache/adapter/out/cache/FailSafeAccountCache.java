package br.com.nevvesdev.dcache.adapter.out.cache;

import br.com.nevvesdev.dcache.application.port.out.AccountCache;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.shared.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

public class FailSafeAccountCache implements AccountCache {

    private static final Logger log = LoggerFactory.getLogger(FailSafeAccountCache.class);

    private final AccountCache delegate;
    private final CacheCircuitBreaker circuitBreaker;

    public FailSafeAccountCache(AccountCache delegate, int failureThreshold, Duration openDuration) {
        this(delegate, new CacheCircuitBreaker(failureThreshold, openDuration, Clock.systemUTC()));
    }

    FailSafeAccountCache(AccountCache delegate, CacheCircuitBreaker circuitBreaker) {
        this.delegate = Preconditions.requireNonNull(delegate, "delegate");
        this.circuitBreaker = Preconditions.requireNonNull(circuitBreaker, "circuitBreaker");
    }

    @Override
    public Optional<Account> find(AccountId id) {
        Preconditions.requireNonNull(id, "id");
        if (!circuitBreaker.allowRequest()) {
            return Optional.empty();
        }
        try {
            Optional<Account> result = delegate.find(id);
            circuitBreaker.recordSuccess();
            return result;
        } catch (RuntimeException exception) {
            circuitBreaker.recordFailure();
            log.warn("Cache lookup failed for account {}; falling back to the source of truth", id, exception);
            return Optional.empty();
        }
    }

    @Override
    public void put(Account account) {
        Preconditions.requireNonNull(account, "account");
        if (!circuitBreaker.allowRequest()) {
            return;
        }
        try {
            delegate.put(account);
            circuitBreaker.recordSuccess();
        } catch (RuntimeException exception) {
            circuitBreaker.recordFailure();
            log.warn("Cache write failed for account {}; continuing without caching", account.id(), exception);
        }
    }
}
