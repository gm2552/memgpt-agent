package com.java.example.chatbot;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Profile("mcp")
public class MCPChatEngine implements ChatEngine {

    private final McpSyncClient client;

    private final ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;

    private String userId;

    public MCPChatEngine(List<McpSyncClient> mcpSyncClients, ChatClient.Builder chatClientBuilder) {
        this.client = mcpSyncClients.getFirst();
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public void initialize(String userId) {
        this.userId = userId;

        ToolCallbackProvider provider = new SyncMcpToolCallbackProvider(client);

        FunctionCallback[] callbacks = provider.getToolCallbacks();

        // get the prompts
        McpSchema.GetPromptResult result = client.getPrompt(new McpSchema.GetPromptRequest("MemGPTSystemPrompt", Map.of()));

        McpSchema.TextContent systemPromptContent = (McpSchema.TextContent)result.messages().getFirst().content();

        String systemPromptText = systemPromptContent.text().replace("$agentNameVal", userId);

        chatClient = chatClientBuilder.defaultTools(callbacks).defaultSystem(systemPromptText).build();

        System.out.println("Welcome.  Let's start a conversation.");

    }

    @Override
    public String chat(String message) {

        ChatResponse resp = chatClient.prompt().user(message).call().chatResponse();

        return resp.getResult().getOutput().getText();
    }

}
