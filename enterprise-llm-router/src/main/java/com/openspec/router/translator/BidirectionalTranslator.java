package com.openspec.router.translator;

import com.openspec.router.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BidirectionalTranslator {

    public Map<String, Object> chatToResponse(Map<String, Object> chatRequest) {
        // PoC: produce minimal Response API compatible map
        return Map.of(
                "thread_id", chatRequest.getOrDefault("conversationId", "default-thread"),
                "content", chatRequest.getOrDefault("content", "")
        );
    }

    public Map<String, Object> responseToChat(Map<String, Object> response) {
        // PoC: convert response map to chat-like structure
        return Map.of(
                "conversationId", response.getOrDefault("thread_id", "default-thread"),
                "content", response.getOrDefault("content", "")
        );
    }

    public List<ChatMessage> flattenThread(List<Map<String, Object>> threadMessages) {
        List<ChatMessage> out = new ArrayList<>();
        for (Map<String, Object> m : threadMessages) {
            String role = (String) m.getOrDefault("role", "user");
            String content = (String) m.getOrDefault("content", "");
            out.add(new ChatMessage(role, content));
        }
        return out;
    }
}
