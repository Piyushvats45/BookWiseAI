package com.bookwise.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookwise.auth.CurrentUser;
import com.bookwise.chat.ChatDtos.AskRequest;
import com.bookwise.chat.ChatDtos.MessageDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/books/{bookId}")
public class ChatController {

    private final RagService rag;

    public ChatController(RagService rag) {
        this.rag = rag;
    }

    /** Ask a question about this book. Returns the assistant's answer with citations. */
    @PostMapping("/chat")
    public MessageDto ask(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookId,
            @Valid @RequestBody AskRequest request) {
        return rag.ask(CurrentUser.id(jwt), bookId, request.question());
    }

    @GetMapping("/messages")
    public List<MessageDto> history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookId) {
        return rag.history(CurrentUser.id(jwt), bookId);
    }

    @DeleteMapping("/messages")
    public ResponseEntity<Void> clear(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID bookId) {
        rag.clearHistory(CurrentUser.id(jwt), bookId);
        return ResponseEntity.noContent().build();
    }
}
