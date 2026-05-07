package com.openspec.router.adapter;

import com.openspec.router.model.ChatMessage;
import com.openspec.router.service.ChatMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ChatCompletionAdapter implements ProtocolAdapter {

    private final ChatMemoryService chatMemoryService;

    // ChatClient from Spring AI can be injected when available; keep optional for PoC
    @Autowired(required = false)
    private Object chatClient;

    @Autowired
    public ChatCompletionAdapter(ChatMemoryService chatMemoryService) {
        this.chatMemoryService = chatMemoryService;
    }

    @Override
    public void validate(Object request) throws AdapterValidationException {
        if (request == null) {
            throw new AdapterValidationException("Request cannot be null");
        }
    }

    @Override
    public Object requestToInternal(Object request) {
        // Minimal PoC: expect request as a Map with conversationId, role, content
        if (request instanceof Map<?, ?> m) {
            String conversationId = (String) m.getOrDefault("conversationId", "default");
            String role = (String) m.getOrDefault("role", "user");
            String content = (String) m.getOrDefault("content", "");

            ChatMessage message = new ChatMessage(role, content);
            chatMemoryService.addMessage(conversationId, message);

            Map<String, Object> internal = new HashMap<>();
            internal.put("conversationId", conversationId);
            internal.put("message", message);
            return internal;
        }
        return request;
    }

    @Override
    public Object responseToExternal(Object response) {
        // PoC: return a simple map with generated content placeholder
        Map<String, Object> out = new HashMap<>();
        out.put("id", "stub-response");
        out.put("content", "This is a placeholder response from ChatCompletionAdapter.");
        return out;
    }
}
