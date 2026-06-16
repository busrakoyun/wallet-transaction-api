package com.hesap.wallet.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standardized error body returned for every failure: {@code { "errorCode": ..., "message": ... }}.
 *
 * <p>{@code errorCode} is pinned with {@link JsonProperty} so it stays camelCase as the spec
 * requires, overriding the application-wide snake_case naming strategy.
 */
public record ErrorResponse(
        @JsonProperty("errorCode") String errorCode,
        String message
) {
}
