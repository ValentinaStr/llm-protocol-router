package com.openspec.router.service;

import com.openspec.router.model.ChatMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryChatMemoryServiceTest {

    @Test
    void addRetrieveClearConversation() {
        InMemoryChatMemoryService svc = new InMemoryChatMemoryService();

        ChatMessage m1 = new ChatMessage("user", "hello world");
        svc.addMessage("conv-1", m1);

        assertThat(svc.getMessages("conv-1")).hasSize(1).containsExactly(m1);

        svc.clearConversation("conv-1");
        assertThat(svc.getMessages("conv-1")).isEmpty();
    }
}
