package com.bookwise.book;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ingestion runs in memory, so a restart can leave books stuck in PROCESSING forever.
 * On startup, mark them FAILED (and drop any partial vectors) so the user can re-upload.
 */
@Component
public class IngestionRecovery {

    private static final Logger log = LoggerFactory.getLogger(IngestionRecovery.class);

    private final BookRepository books;
    private final IngestionService ingestion;

    public IngestionRecovery(BookRepository books, IngestionService ingestion) {
        this.books = books;
        this.ingestion = ingestion;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedIngestions() {
        for (Book book : books.findByStatus(BookStatus.PROCESSING)) {
            log.warn("Marking interrupted book {} as FAILED", book.getId());
            ingestion.removeVectors(book.getId());
            book.setStatus(BookStatus.FAILED);
            book.setErrorMessage("Processing was interrupted by a server restart. Delete this book and upload it again.");
            books.save(book);
        }
    }
}
