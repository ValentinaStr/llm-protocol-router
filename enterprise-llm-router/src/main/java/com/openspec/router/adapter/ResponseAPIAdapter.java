package com.openspec.router.adapter;

import com.openspec.router.model.ChatMessage;
import com.openspec.router.service.ChatMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ResponseAPIAdapter implements ProtocolAdapter {

    private final ChatMemoryService chatMemoryService;

    @Autowired(required = false)
    private Object chatClient;

    @Autowired
    public ResponseAPIAdapter(ChatMemoryService chatMemoryService) {
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
        // Expect request to be a Map with thread_id, assistant_id, messages
        if (request instanceof Map<?, ?> m) {
            String threadId = (String) m.getOrDefault("thread_id", "default-thread");
            String assistantId = (String) m.getOrDefault("assistant_id", "default-assistant");
            String role = (String) m.getOrDefault("role", "user");
            String content = (String) m.getOrDefault("content", "");

            String conversationId = threadId; // mapping for PoC
            ChatMessage msg = new ChatMessage(role, content);
            chatMemoryService.addMessage(conversationId, msg);

            Map<String, Object> internal = new HashMap<>();
            internal.put("conversationId", conversationId);
            internal.put("assistantId", assistantId);
            internal.put("message", msg);
            return internal;
        }
        return request;
    }

    @Override
    public Object responseToExternal(Object response) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", "stub-response");
        out.put("thread_id", "default-thread");
        out.put("content", "Placeholder Response API output.");
        return out;
    }
}
