package com.bookwise.book;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bookwise.auth.CurrentUser;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService books;

    public BookController(BookService books) {
        this.books = books;
    }

    @GetMapping
    public List<BookDto> list(@AuthenticationPrincipal Jwt jwt) {
        return books.list(CurrentUser.id(jwt)).stream().map(BookDto::from).toList();
    }

    /** Returns 202: the book is validated and stored, indexing continues in the background. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookDto> upload(@AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        Book book = books.upload(CurrentUser.id(jwt), file, title);
        return ResponseEntity.accepted().body(BookDto.from(book));
    }

    @GetMapping("/{id}")
    public BookDto get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return BookDto.from(books.getOwned(CurrentUser.id(jwt), id));
    }

    /** Streams the original PDF so the UI can open a cited page. */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        Book book = books.getOwned(CurrentUser.id(jwt), id);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(book.getOriginalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(books.file(book));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        books.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
