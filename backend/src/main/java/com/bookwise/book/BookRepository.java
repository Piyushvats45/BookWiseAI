package com.bookwise.book;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, UUID> {

    List<Book> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Book> findByIdAndUserId(UUID id, UUID userId);

    Optional<Book> findByUserIdAndSha256(UUID userId, String sha256);

    List<Book> findByStatus(BookStatus status);
}
