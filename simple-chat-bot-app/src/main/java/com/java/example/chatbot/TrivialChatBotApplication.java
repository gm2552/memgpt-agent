package com.java.example.chatbot;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;


@SpringBootApplication(exclude = {OpenAiAutoConfiguration.class})
public class TrivialChatBotApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(TrivialChatBotApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean
    public CommandLineRunner restCommandLineRunner(ChatEngine chatEngine) {

        return args -> {

            try(Scanner scanner = new Scanner(System.in)) {

                String input;

                System.out.println("Welcome to the MemGTP Chatbot.  What is your user id?");
                System.out.print("=> ");
                input = scanner.nextLine();

                chatEngine.initialize(input);

                while (true) {
                    System.out.print("=> ");
                    input = scanner.nextLine();

                    if (input.equalsIgnoreCase("exit")) {
                        break; // Exit the loop if the user types "exit"
                    }

                    try {
                        String chatResult = chatEngine.chat(input);

                        System.out.println(chatResult);
                    }
                    catch(Exception e) {
                        System.err.println("Chat error occurred.  Please try again or come back and try again later: " + e.getMessage());
                    }
                }
            }
        };
    }

    @Profile("mcp")
    @Bean
    public CommandLineRunner mcpCommandLineRunner(List<McpSyncClient> mcpSyncClients, ChatClient.Builder chatClientBuilder) {

        return args -> {

            McpSyncClient client = mcpSyncClients.getFirst();


            ToolCallbackProvider provider = new SyncMcpToolCallbackProvider(client);

            FunctionCallback[] callbacks = provider.getToolCallbacks();

            // get the prompts
            McpSchema.GetPromptResult result = client.getPrompt(new McpSchema.GetPromptRequest("MemGPTSystemPrompt", Map.of()));

            McpSchema.TextContent systemPromptContent = (McpSchema.TextContent)result.messages().getFirst().content();

            // TODO: Get the agent id from the user.
            String systemPromptText = systemPromptContent.text().replace("$agentNameVal", "gm2552");

            ChatClient chatClient = chatClientBuilder.defaultTools(callbacks).defaultSystem(systemPromptText).build();

            Scanner scanner = new Scanner(System.in);
            String input;

            while (true) {
                System.out.print("=> ");
                input = scanner.nextLine();

                if (input.equalsIgnoreCase("exit")) {
                    break; // Exit the loop if the user types "exit"
                }

                ChatResponse resp = chatClient.prompt().user(input).call().chatResponse();

                System.out.println(resp.getResult().getOutput().getText());
                System.out.println("Context Window Usage: " + resp.getMetadata().getUsage().getTotalTokens());
            }
            scanner.close();

        };
    }

    public record AgentCreateRequest(String agentName, int contextWindowSize) {}

}
