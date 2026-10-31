package com.bookwise.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import com.bookwise.common.ApiException;
import com.bookwise.config.AppProperties;

class PdfValidatorTest {

    private final PdfValidator validator = new PdfValidator(
            new AppProperties(null, new AppProperties.Storage("target/test-books", 1_000_000, 5), null, null));

    @Test
    void acceptsRealPdfAndReportsPageCount() throws Exception {
        assertEquals(2, validator.validate(pdfWithPages(2)).pageCount());
    }

    @Test
    void rejectsNonPdfContent() {
        byte[] notPdf = "just some text".getBytes(StandardCharsets.UTF_8);
        assertThrows(ApiException.class, () -> validator.validate(notPdf));
    }

    @Test
    void rejectsEmptyFile() {
        assertThrows(ApiException.class, () -> validator.validate(new byte[0]));
    }

    @Test
    void rejectsPdfHeaderWithGarbageBody() {
        byte[] fake = "%PDF-1.7\nthis is not a real pdf".getBytes(StandardCharsets.UTF_8);
        assertThrows(ApiException.class, () -> validator.validate(fake));
    }

    @Test
    void rejectsTooManyPages() throws Exception {
        byte[] sixPages = pdfWithPages(6);
        assertThrows(ApiException.class, () -> validator.validate(sixPages));
    }

    private static byte[] pdfWithPages(int pages) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
