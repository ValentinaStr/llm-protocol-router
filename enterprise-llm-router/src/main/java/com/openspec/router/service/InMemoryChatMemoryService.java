package com.openspec.router.service;

import com.openspec.router.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class InMemoryChatMemoryService implements ChatMemoryService {
    private final ConcurrentMap<String, List<ChatMessage>> store = new ConcurrentHashMap<>();

    @Override
    public void addMessage(String conversationId, ChatMessage message) {
        store.computeIfAbsent(conversationId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(message);
    }

    @Override
    public List<ChatMessage> getMessages(String conversationId) {
        return store.getOrDefault(conversationId, Collections.emptyList());
    }

    @Override
    public void clearConversation(String conversationId) {
        store.remove(conversationId);
    }
}
