package com.bhartiyasaas.stayfile.exception;

/**
 * An external provider (payments, e-sign, storage, messaging) is not configured, not
 * implemented yet, or failed. Mapped to HTTP 503 so callers never mistake it for success.
 */
public class ProviderUnavailableException extends RuntimeException {

    public ProviderUnavailableException(String message) {
        super(message);
    }

    public ProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
