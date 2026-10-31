package com.bookwise.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByBookIdAndUserIdOrderByCreatedAtAsc(UUID bookId, UUID userId);

    List<ChatMessage> findByBookIdAndUserIdOrderByCreatedAtDesc(UUID bookId, UUID userId, Pageable pageable);

    @Transactional
    void deleteByBookIdAndUserId(UUID bookId, UUID userId);
}
