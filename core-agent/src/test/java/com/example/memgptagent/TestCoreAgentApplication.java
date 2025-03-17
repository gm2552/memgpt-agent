package com.example.memgptagent;

import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.mockito.Mockito.mock;

@SpringBootApplication(
        exclude = { OpenAiAutoConfiguration.class}
        )
public class TestCoreAgentApplication {


    @Bean
    ChatClient chatClient() {
        return mock(ChatClient.class);
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return mock(EmbeddingModel.class);
    }

    @Profile("postgresql")
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {

        return new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"));
    }

}
