package com.bookwise.chat;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    private UUID id;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** JSON array of citations (assistant messages only). */
    @Column(columnDefinition = "text")
    private String citations;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static ChatMessage of(UUID bookId, UUID userId, MessageRole role, String content, String citations,
            Instant createdAt) {
        ChatMessage m = new ChatMessage();
        m.id = UUID.randomUUID();
        m.bookId = bookId;
        m.userId = userId;
        m.role = role;
        m.content = content;
        m.citations = citations;
        m.createdAt = createdAt;
        return m;
    }
}
