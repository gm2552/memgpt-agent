package com.java.example.chatbot;



import com.java.example.chatbot.advisors.MemGPTMessageChatMemoryAdvisor;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.function.Function;

@Component
@Profile("advisor")
public class ChatMemoryAdvisorEngine implements ChatEngine {


    private final McpSyncClient client;

    private final ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;

    private String userId;

    public ChatMemoryAdvisorEngine(List<McpSyncClient> mcpSyncClients, ChatClient.Builder chatClientBuilder) {
        this.client = mcpSyncClients.getFirst();
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public void initialize(String userId) {

        this.userId = userId;

        chatClient = chatClientBuilder
                .defaultAdvisors(new MemGPTMessageChatMemoryAdvisor(client, userId))
                .build();

        System.out.println("Welcome.  Let's start a conversation.");
    }

    @Override
    public String chat(String message) {
        ChatResponse resp = chatClient.prompt().user(message).call().chatResponse();

        return resp.getResult().getOutput().getText();
    }

}
