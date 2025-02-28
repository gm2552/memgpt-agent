package com.example.memgptagent;

import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@SpringBootApplication(exclude = {
        OpenAiAutoConfiguration.class, PgVectorStoreAutoConfiguration.class})
public class TestCoreAgentApplication {

    @Bean
    ChatClient chatClient() {
        return mock(ChatClient.class);
    }
}
