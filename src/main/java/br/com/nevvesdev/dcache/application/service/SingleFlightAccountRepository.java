package br.com.nevvesdev.dcache.application.service;

import br.com.nevvesdev.dcache.application.port.out.AccountRepository;
import br.com.nevvesdev.dcache.domain.model.Account;
import br.com.nevvesdev.dcache.domain.model.AccountId;
import br.com.nevvesdev.dcache.domain.shared.Preconditions;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SingleFlightAccountRepository implements AccountRepository {

    private final AccountRepository delegate;
    private final ConcurrentMap<AccountId, CompletableFuture<Optional<Account>>> inFlight = new ConcurrentHashMap<>();

    public SingleFlightAccountRepository(AccountRepository delegate) {
        this.delegate = Preconditions.requireNonNull(delegate, "delegate");
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        Preconditions.requireNonNull(id, "id");
        CompletableFuture<Optional<Account>> flight = new CompletableFuture<>();
        CompletableFuture<Optional<Account>> running = inFlight.putIfAbsent(id, flight);
        if (running != null) {
            return await(running);
        }
        return lead(id, flight);
    }

    private Optional<Account> lead(AccountId id, CompletableFuture<Optional<Account>> flight) {
        try {
            Optional<Account> result = delegate.findById(id);
            flight.complete(result);
            return result;
        } catch (Throwable failure) {
            flight.completeExceptionally(failure);
            throw failure;
        } finally {
            inFlight.remove(id, flight);
        }
    }

    private static Optional<Account> await(CompletableFuture<Optional<Account>> flight) {
        try {
            return flight.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }
}
