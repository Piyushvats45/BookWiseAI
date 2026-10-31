package com.bookwise.book;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.bookwise.common.ApiException;
import com.bookwise.config.AppProperties;

/** Rejects anything that is not a real, readable, unencrypted PDF within the configured limits. */
@Component
public class PdfValidator {

    public record PdfInfo(int pageCount) {
    }

    private final AppProperties props;

    public PdfValidator(AppProperties props) {
        this.props = props;
    }

    public PdfInfo validate(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw ApiException.badRequest("The uploaded file is empty.");
        }
        if (bytes.length > props.storage().maxFileSizeBytes()) {
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE,
                    "The file is too large. The limit is " + (props.storage().maxFileSizeBytes() / (1024 * 1024)) + " MB.");
        }
        if (!hasPdfHeader(bytes)) {
            throw ApiException.badRequest("This file is not a PDF.");
        }
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            if (doc.isEncrypted()) {
                throw ApiException.badRequest("Encrypted PDFs are not supported. Remove the protection and try again.");
            }
            int pages = doc.getNumberOfPages();
            if (pages == 0) {
                throw ApiException.badRequest("The PDF has no pages.");
            }
            if (pages > props.storage().maxPages()) {
                throw ApiException.badRequest("The PDF has too many pages (limit " + props.storage().maxPages() + ").");
            }
            return new PdfInfo(pages);
        } catch (InvalidPasswordException e) {
            throw ApiException.badRequest("Password-protected PDFs are not supported.");
        } catch (IOException e) {
            throw ApiException.badRequest("The PDF is corrupted or could not be read.");
        }
    }

    /** The "%PDF-" marker must appear within the first 1024 bytes (per the PDF spec's tolerance). */
    private static boolean hasPdfHeader(byte[] bytes) {
        int limit = Math.min(bytes.length, 1024);
        String head = new String(bytes, 0, limit, StandardCharsets.ISO_8859_1);
        return head.contains("%PDF-");
    }
}
