package br.com.nevvesdev.dcache.application.exception;

public class CacheUnavailableException extends RuntimeException {

    public CacheUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
