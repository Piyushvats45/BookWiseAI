package com.bookwise.book;

public enum BookStatus {
    /** Uploaded and validated; text extraction, chunking and embedding are running. */
    PROCESSING,
    /** Fully indexed in pgvector; ready for questions. */
    READY,
    /** Ingestion failed; see Book.errorMessage. */
    FAILED
}
