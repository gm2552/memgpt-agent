package com.example.memgptagent.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.server.McpServerFeatures;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class McpServerConfig {

    private static final String SYSTEM_PROMPT_MESSAGE_CONTENT = """
            The MemGPT takes a user prompt and keeps a full history of the conversation context.
            The flow takes a single User prompt and passes the content as is to the chat tool and returns
            the tool response as is.  The agentName parameter should set to $agentNameVal.
            """;

    @Bean
    public List<ToolCallback> toolCallback(MemGPTTools tools) {
        return Arrays.stream(ToolCallbacks.from(tools)).toList();
    }

    @Bean
    public List<McpServerFeatures.SyncPromptRegistration> prompts() {

        Prompt systemPrompt = new Prompt("MemGPTSystemPrompt",
                "The recommended system prompt to help describe to the LLM how to use the MemGPT toos.",
                List.of());


        McpServerFeatures.SyncPromptRegistration promptReg = new McpServerFeatures.SyncPromptRegistration(systemPrompt,
                (promptRequest -> {

                    McpSchema.PromptMessage systemMessage = new McpSchema.PromptMessage(McpSchema.Role.USER,
                            new McpSchema.TextContent(SYSTEM_PROMPT_MESSAGE_CONTENT));

                    return new McpSchema.GetPromptResult("MemGPT chat system prompt", List.of(systemMessage));

                }));

        return List.of(promptReg);
    }

}
