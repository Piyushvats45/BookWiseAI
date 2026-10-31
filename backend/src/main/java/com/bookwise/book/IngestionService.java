package com.bookwise.book;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.bookwise.config.AppProperties;

/**
 * The ingestion pipeline: PDF -> one Document per page -> token-based chunks -> embeddings -> pgvector.
 *
 * Chunks are built per page, so every chunk belongs to exactly one page and citations are exact.
 * Each stored chunk carries user_id and book_id metadata so searches can be filtered to one user's book.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final int EMBED_BATCH = 50;

    /** A failure whose message is safe and useful to show to the user. */
    static class IngestionException extends RuntimeException {
        IngestionException(String message) {
            super(message);
        }
    }

    private final BookRepository books;
    private final VectorStore vectorStore;
    private final AppProperties props;

    public IngestionService(BookRepository books, VectorStore vectorStore, AppProperties props) {
        this.books = books;
        this.vectorStore = vectorStore;
        this.props = props;
    }

    @Async("ingestionExecutor")
    public void ingest(UUID bookId) {
        Book book = books.findById(bookId).orElse(null);
        if (book == null) {
            return;
        }
        long started = System.currentTimeMillis();
        try {
            int chunks = process(book);
            book.setChunkCount(chunks);
            book.setStatus(BookStatus.READY);
            book.setErrorMessage(null);
            log.info("Ingested book {} ({} pages, {} chunks) in {} ms",
                    bookId, book.getPageCount(), chunks, System.currentTimeMillis() - started);
        } catch (Exception e) {
            log.error("Ingestion failed for book {}", bookId, e);
            removeVectors(bookId);
            book.setChunkCount(0);
            book.setStatus(BookStatus.FAILED);
            book.setErrorMessage(e instanceof IngestionException
                    ? e.getMessage()
                    : "Processing failed. Check that the AI provider is configured correctly, then delete and re-upload.");
        }
        books.save(book);
    }

    private int process(Book book) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                new FileSystemResource(Path.of(book.getFilePath())),
                PdfDocumentReaderConfig.builder().withPagesPerDocument(1).build());
        List<Document> pages = reader.read();

        List<Document> pageDocs = new ArrayList<>();
        int position = 0;
        for (Document page : pages) {
            position++;
            String text = clean(page.getText());
            if (text.isBlank()) {
                continue;
            }
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("user_id", book.getUserId().toString());
            metadata.put("book_id", book.getId().toString());
            metadata.put("book_title", book.getTitle());
            metadata.put("file_name", book.getOriginalFilename());
            metadata.put("page_number", pageNumber(page, position));
            pageDocs.add(new Document(text, metadata));
        }
        if (pageDocs.isEmpty()) {
            throw new IngestionException(
                    "No text could be extracted. This looks like a scanned PDF; OCR is not supported yet.");
        }

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(props.rag().chunkSizeTokens())
                .withMinChunkLengthToEmbed(20)
                .build();
        List<Document> chunks = splitter.apply(pageDocs);
        if (chunks.isEmpty()) {
            throw new IngestionException("No usable text chunks were produced from this PDF.");
        }

        for (int i = 0; i < chunks.size(); i += EMBED_BATCH) {
            // add() embeds the batch with the configured EmbeddingModel and inserts the rows.
            vectorStore.add(chunks.subList(i, Math.min(i + EMBED_BATCH, chunks.size())));
        }
        return chunks.size();
    }

    void removeVectors(UUID bookId) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            vectorStore.delete(b.eq("book_id", bookId.toString()).build());
        } catch (Exception e) {
            log.warn("Could not remove vectors for book {}", bookId, e);
        }
    }

    private static int pageNumber(Document page, int fallback) {
        Object value = page.getMetadata().get("page_number");
        return value instanceof Number n ? n.intValue() : fallback;
    }

    /** PostgreSQL text/json columns reject NUL characters, which some PDFs contain. */
    private static String clean(String text) {
        return text == null ? "" : text.replace("\u0000", "").strip();
    }
}
