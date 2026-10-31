package com.bookwise.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bookwise")
public record AppProperties(Jwt jwt, Storage storage, Rag rag, Cors cors) {

    public record Jwt(String secret, Duration expiry) {
    }

    public record Storage(String dir, long maxFileSizeBytes, int maxPages) {
    }

    public record Rag(int topK, double similarityThreshold, int chunkSizeTokens, int historyMessages) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
