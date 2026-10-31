package com.bookwise.book;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bookwise.book.PdfValidator.PdfInfo;
import com.bookwise.common.ApiException;
import com.bookwise.config.AppProperties;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository books;
    private final PdfValidator validator;
    private final IngestionService ingestion;
    private final AppProperties props;

    public BookService(BookRepository books, PdfValidator validator, IngestionService ingestion, AppProperties props) {
        this.books = books;
        this.validator = validator;
        this.ingestion = ingestion;
        this.props = props;
    }

    public List<Book> list(UUID userId) {
        return books.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Ownership-checked lookup. Another user's book looks exactly like a missing one. */
    public Book getOwned(UUID userId, UUID bookId) {
        return books.findByIdAndUserId(bookId, userId)
                .orElseThrow(() -> ApiException.notFound("Book not found."));
    }

    public Book getReady(UUID userId, UUID bookId) {
        Book book = getOwned(userId, bookId);
        if (book.getStatus() == BookStatus.PROCESSING) {
            throw ApiException.conflict("This book is still being processed. Try again in a moment.");
        }
        if (book.getStatus() == BookStatus.FAILED) {
            throw ApiException.conflict("This book could not be processed, so it can't be queried.");
        }
        return book;
    }

    public Book upload(UUID userId, MultipartFile file, String requestedTitle) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("The upload could not be read.");
        }
        PdfInfo info = validator.validate(bytes);

        String sha = sha256(bytes);
        books.findByUserIdAndSha256(userId, sha).ifPresent(existing -> {
            throw ApiException.conflict("You already uploaded this file as \"" + existing.getTitle() + "\".");
        });

        UUID id = UUID.randomUUID();
        String filename = sanitizeFilename(file.getOriginalFilename());
        Path path = Path.of(props.storage().dir(), userId.toString(), id + ".pdf");
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException e) {
            log.error("Could not store upload", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The file could not be stored.");
        }

        Book book = new Book();
        book.setId(id);
        book.setUserId(userId);
        book.setTitle(titleFor(requestedTitle, filename));
        book.setOriginalFilename(filename);
        book.setFilePath(path.toString());
        book.setSizeBytes(bytes.length);
        book.setSha256(sha);
        book.setPageCount(info.pageCount());
        book.setStatus(BookStatus.PROCESSING);
        try {
            book = books.save(book);
        } catch (DataIntegrityViolationException e) {
            deleteQuietly(path);
            throw ApiException.conflict("You already uploaded this file.");
        }

        ingestion.ingest(book.getId());
        return book;
    }

    public void delete(UUID userId, UUID bookId) {
        Book book = getOwned(userId, bookId);
        if (book.getStatus() == BookStatus.PROCESSING) {
            throw ApiException.conflict("This book is still being processed and can't be deleted yet.");
        }
        ingestion.removeVectors(bookId);
        books.delete(book); // chat messages are removed by ON DELETE CASCADE
        deleteQuietly(Path.of(book.getFilePath()));
    }

    public Resource file(Book book) {
        Path path = Path.of(book.getFilePath());
        if (!Files.isReadable(path)) {
            throw ApiException.notFound("The original file is no longer available.");
        }
        return new FileSystemResource(path);
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete {}", path, e);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sanitizeFilename(String original) {
        String name = original == null ? "document.pdf" : original;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("\\p{Cntrl}", "").strip();
        if (name.isEmpty()) {
            name = "document.pdf";
        }
        return name.length() > 300 ? name.substring(0, 300) : name;
    }

    private static String titleFor(String requested, String filename) {
        String title = requested == null ? "" : requested.strip();
        if (title.isEmpty()) {
            title = filename.replaceAll("(?i)\\.pdf$", "");
        }
        return title.length() > 300 ? title.substring(0, 300) : title;
    }
}
