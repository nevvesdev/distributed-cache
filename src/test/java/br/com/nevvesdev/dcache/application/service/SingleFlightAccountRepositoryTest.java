package br.com.nevvesdev.dcache.application.service;

import br.com.nevvesdev.dcache.application.port.out.AccountRepository;
import br.com.nevvesdev.dcache.domain.exception.InvalidValueException;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.model.Balance;
import br.com.nevvesdev.dcache.domain.model.CreditLimit;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SingleFlightAccountRepositoryTest {

    private static final int FOLLOWERS = 5;

    private static Account anAccount() {
        return new Account(AccountId.generate(), Balance.of("10.00"), CreditLimit.of("20.00"), Instant.parse("2026-10-17T12:00:00Z"));
    }

    @Test
    void shouldCollapseConcurrentLookupsIntoSingleDelegateCall() throws Exception {
        Account account = anAccount();
        BlockingRepository delegate = new BlockingRepository(() -> Optional.of(account));
        SingleFlightAccountRepository repository = new SingleFlightAccountRepository(delegate);
        ExecutorService executor = Executors.newFixedThreadPool(FOLLOWERS + 1);
        try {
            List<Future<Optional<Account>>> results = lookupConcurrently(executor, repository, delegate, account.id());

            for (Future<Optional<Account>> result : results) {
                assertThat(result.get(5, TimeUnit.SECONDS)).contains(account);
            }
            assertThat(delegate.calls()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldShareFailureWithConcurrentCallers() throws Exception {
        AccountId id = AccountId.generate();
        BlockingRepository delegate = new BlockingRepository(() -> {
            throw new IllegalStateException("database unavailable");
        });
        SingleFlightAccountRepository repository = new SingleFlightAccountRepository(delegate);
        ExecutorService executor = Executors.newFixedThreadPool(FOLLOWERS + 1);
        try {
            List<Future<Optional<Account>>> results = lookupConcurrently(executor, repository, delegate, id);

            for (Future<Optional<Account>> result : results) {
                assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class);
            }
            assertThat(delegate.calls()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldAllowNewLookupAfterFailure() {
        Account account = anAccount();
        AtomicInteger calls = new AtomicInteger();
        AccountRepository delegate = id -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("database unavailable");
            }
            return Optional.of(account);
        };
        SingleFlightAccountRepository repository = new SingleFlightAccountRepository(delegate);

        assertThatThrownBy(() -> repository.findById(account.id())).isInstanceOf(IllegalStateException.class);

        assertThat(repository.findById(account.id())).contains(account);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void shouldQueryDelegateAgainForSequentialLookups() {
        Account account = anAccount();
        AtomicInteger calls = new AtomicInteger();
        AccountRepository delegate = id -> {
            calls.incrementAndGet();
            return Optional.of(account);
        };
        SingleFlightAccountRepository repository = new SingleFlightAccountRepository(delegate);

        repository.findById(account.id());
        repository.findById(account.id());

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void shouldNotShareResultsBetweenDifferentAccounts() {
        AtomicInteger calls = new AtomicInteger();
        AccountRepository delegate = id -> {
            calls.incrementAndGet();
            return Optional.empty();
        };
        SingleFlightAccountRepository repository = new SingleFlightAccountRepository(delegate);

        assertThat(repository.findById(AccountId.generate())).isEmpty();
        assertThat(repository.findById(AccountId.generate())).isEmpty();

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void shouldRejectNullArguments() {
        SingleFlightAccountRepository repository = new SingleFlightAccountRepository(id -> Optional.empty());

        assertThatThrownBy(() -> repository.findById(null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new SingleFlightAccountRepository(null)).isInstanceOf(InvalidValueException.class);
    }

    private static List<Future<Optional<Account>>> lookupConcurrently(
        ExecutorService executor,
        SingleFlightAccountRepository repository,
        BlockingRepository delegate,
        AccountId id) throws InterruptedException {

        List<Future<Optional<Account>>> results = new ArrayList<>();
        List<Thread> followerThreads = new CopyOnWriteArrayList<>();

        results.add(executor.submit(() -> repository.findById(id)));
        delegate.awaitStarted();

        for (int i = 0; i < FOLLOWERS; i++) {
            results.add(executor.submit(() -> {
                followerThreads.add(Thread.currentThread());
                return repository.findById(id);
            }));
        }

        awaitUntil(() -> followerThreads.size() == FOLLOWERS
            && followerThreads.stream().allMatch(thread -> thread.getState() == Thread.State.WAITING));

        delegate.release();
        return results;
    }

    private static void awaitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("condition not met within 5 seconds");
            }
            Thread.sleep(10);
        }
    }

    private static final class BlockingRepository implements AccountRepository {

        private final Supplier<Optional<Account>> outcome;
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch released = new CountDownLatch(1);
        private final AtomicInteger calls = new AtomicInteger();

        BlockingRepository(Supplier<Optional<Account>> outcome) {
            this.outcome = outcome;
        }

        void awaitStarted() throws InterruptedException {
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        }

        void release() {
            released.countDown();
        }

        int calls() {
            return calls.get();
        }

        @Override
        public Optional<Account> findById(AccountId id) {
            calls.incrementAndGet();
            started.countDown();
            try {
                if (!released.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("release signal not received");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            return outcome.get();
        }
    }
}
