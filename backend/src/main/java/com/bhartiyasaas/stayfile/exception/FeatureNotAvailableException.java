package com.bhartiyasaas.stayfile.exception;

/**
 * Thrown when an operation is not allowed for the caller's organization type
 * (e.g. landlord management for an OWNER organization). Mapped to HTTP 403.
 */
public class FeatureNotAvailableException extends RuntimeException {
    public FeatureNotAvailableException(String message) {
        super(message);
    }
}
