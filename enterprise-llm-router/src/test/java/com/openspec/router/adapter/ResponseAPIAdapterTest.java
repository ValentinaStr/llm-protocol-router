package com.openspec.router.adapter;

import com.openspec.router.model.ChatMessage;
import com.openspec.router.service.InMemoryChatMemoryService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponseAPIAdapterTest {

    @Test
    void requestToInternalStoresMessageAndReturnsInternalMap() {
        InMemoryChatMemoryService memory = new InMemoryChatMemoryService();
        ResponseAPIAdapter adapter = new ResponseAPIAdapter(memory);

        Map<String, Object> request = Map.of(
                "thread_id", "thread-123",
                "assistant_id", "assistant-1",
                "role", "user",
                "content", "hello"
        );

        Object internal = adapter.requestToInternal(request);
        assertThat(internal).isInstanceOf(Map.class);
        Map<?, ?> im = (Map<?, ?>) internal;
        assertThat(im.get("conversationId")).isEqualTo("thread-123");
        assertThat(im.get("assistantId")).isEqualTo("assistant-1");
        assertThat(im.get("message")).isInstanceOf(ChatMessage.class);

        // verify memory stored
        var msgs = memory.getMessages("thread-123");
        assertThat(msgs).hasSize(1);
        assertThat(msgs.get(0).getContent()).isEqualTo("hello");
    }

    @Test
    void responseToExternalProducesPlaceholderMap() {
        InMemoryChatMemoryService memory = new InMemoryChatMemoryService();
        ResponseAPIAdapter adapter = new ResponseAPIAdapter(memory);

        Object out = adapter.responseToExternal(Map.of("dummy", "x"));
        assertThat(out).isInstanceOf(Map.class);
        Map<?, ?> m = (Map<?, ?>) out;
        assertThat(m.get("thread_id")).isNotNull();
        assertThat(m.get("content")).isNotNull();
    }
}
