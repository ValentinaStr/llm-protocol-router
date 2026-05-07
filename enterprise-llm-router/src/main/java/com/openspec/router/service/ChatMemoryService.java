package com.openspec.router.service;

import com.openspec.router.model.ChatMessage;

import java.util.List;

public interface ChatMemoryService {
    void addMessage(String conversationId, ChatMessage message);

    List<ChatMessage> getMessages(String conversationId);

    void clearConversation(String conversationId);
}
