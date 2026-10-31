package com.bookwise.book;

import java.time.Instant;
import java.util.UUID;

public record BookDto(
        UUID id,
        String title,
        String originalFilename,
        long sizeBytes,
        int pageCount,
        int chunkCount,
        BookStatus status,
        String errorMessage,
        Instant createdAt) {

    public static BookDto from(Book b) {
        return new BookDto(b.getId(), b.getTitle(), b.getOriginalFilename(), b.getSizeBytes(),
                b.getPageCount(), b.getChunkCount(), b.getStatus(), b.getErrorMessage(), b.getCreatedAt());
    }
}
