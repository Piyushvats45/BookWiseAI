package com.bookwise.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ChatDtos {

    private ChatDtos() {
    }

    public record AskRequest(@NotBlank @Size(max = 2000) String question) {
    }

    /** A source the answer relied on: which book, which page, and the passage that was retrieved. */
    public record Citation(int index, UUID bookId, String bookTitle, int page, String snippet, Double score) {
    }

    public record MessageDto(UUID id, String role, String content, List<Citation> citations, Instant createdAt) {
    }
}
