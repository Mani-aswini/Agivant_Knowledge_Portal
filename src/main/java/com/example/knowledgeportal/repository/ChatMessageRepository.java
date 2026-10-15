package com.example.knowledgeportal.repository;

import com.example.knowledgeportal.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUsernameOrderByCreatedAtAsc(String username);

    List<ChatMessage> findByUsernameAndConversationIdOrderByCreatedAtAsc(String username, String conversationId);

    void deleteByUsername(String username);

    void deleteByUsernameAndConversationId(String username, String conversationId);
}
