package com.bhartiyasaas.stayfile.exception;

import lombok.Getter;

@Getter
public class IntegrationException extends RuntimeException {

    private final String provider;
    private final int statusCode;
    private final String rawResponseBody;
    private final String userMessage;

    public IntegrationException(String provider, int statusCode, String rawResponseBody, String userMessage) {
        super(String.format("[%s Error %d]: %s", provider, statusCode, userMessage));
        this.provider = provider;
        this.statusCode = statusCode;
        this.rawResponseBody = rawResponseBody;
        this.userMessage = userMessage;
    }

    public IntegrationException(String provider, String userMessage, Throwable cause) {
        super(String.format("[%s Connection Error]: %s", provider, userMessage), cause);
        this.provider = provider;
        this.statusCode = 502;
        this.rawResponseBody = null;
        this.userMessage = userMessage;
    }
}
