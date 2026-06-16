package com.hesap.wallet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aligns springdoc/Swagger schema generation with the API's actual wire format.
 *
 * <p>The application serializes JSON in snake_case (via
 * {@code spring.jackson.property-naming-strategy}), but swagger-core's default model
 * resolver derives schema property names from the Java fields (camelCase). Backing the
 * {@link ModelResolver} with the application's Jackson {@link ObjectMapper} makes the
 * generated schema match real payloads (e.g. {@code sender_account_id}) while still
 * honoring explicit {@code @JsonProperty} names such as {@code errorCode}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public ModelResolver modelResolver(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper);
    }
}
