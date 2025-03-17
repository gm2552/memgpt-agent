package com.java.example.chatbot.advisors;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Profile("mcpadvisor")
@Component
public class MemGPTMCPChatMemory implements MemGPTChatMemory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemGPTMCPChatMemory.class);

    private static final String RECALL_TOOL_NAME = "recallContext";

    private static final String APPEND_TOOL_NAME = "appendContext";

    private static final String CLEAR_TOOL_NAME = "clear";

    private static final String AGENT_NAME_ARG = "agentName";

    private static final String PROMPT_MESSAGE_ARG = "promptMessage";

    private static final String MESSAGE_ARG = "messages";

    private final McpSyncClient client;

    private UserMessage userMessage;

    public MemGPTMCPChatMemory(List<McpSyncClient> mcpSyncClients) {
        this.client = mcpSyncClients.get(0);
    }


    @Override
    public void add(String conversationId, List<Message> messages) {

        var args = Map.of(AGENT_NAME_ARG, conversationId, MESSAGE_ARG, messages);

        McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest(APPEND_TOOL_NAME, args);

        try{
            McpSchema.CallToolResult result = client.callTool(callToolRequest);

            if (result.isError())
                LOGGER.warn("Error appending context.  Unknown error");
        }
        catch (Exception e) {
            LOGGER.warn("Error appending context: {}", e.getMessage());
        }

    }

    @Override
    public List<Message> get(String conversationId, int lastN) {

        // The user message is passes to update the context's core memory are to
        // retrieve any other relevant information from "virtual" memory.
        var args = Map.of(AGENT_NAME_ARG, conversationId, PROMPT_MESSAGE_ARG, userMessage);

        McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest(RECALL_TOOL_NAME, args);

        try{

            McpSchema.CallToolResult result = client.callTool(callToolRequest);

            if (result.isError())
                LOGGER.warn("Error recalling context.  Unknown error");
            else {

                if (!result.content().isEmpty()) {

                    // there should be one and only one content entry if there was not an error
                    String content = ((McpSchema.TextContent)result.content().getFirst()).text();

                    return  MessageUtils.deserializeMessagesFromJSONString(content);
                }
            }

        }
        catch (Exception e) {
            LOGGER.warn("Error recalling context: {}", e.getMessage());

        }

        return List.of();

    }

    @Override
    public void clear(String conversationId) {
        var args = Map.of(AGENT_NAME_ARG, (Object)conversationId);

        McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest(CLEAR_TOOL_NAME, args);

        try{
            McpSchema.CallToolResult result = client.callTool(callToolRequest);

            if (result.isError())
                LOGGER.warn("Error clearing context memory.  Unknown error");
        }
        catch (Exception e) {
            LOGGER.warn("Error clearing context memory: {}", e.getMessage());
        }
    }

    public void setUserMessage(UserMessage userMessage) {
        this.userMessage = userMessage;
    }

}
