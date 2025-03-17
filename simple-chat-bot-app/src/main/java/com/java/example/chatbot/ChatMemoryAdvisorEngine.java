package com.java.example.chatbot;



import com.java.example.chatbot.advisors.MemGPTChatMemory;
import com.java.example.chatbot.advisors.MemGPTMessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mcpadvisor || restadvisor")
public class ChatMemoryAdvisorEngine implements ChatEngine {


    private final MemGPTChatMemory chatMemory;

    private final ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;

    private String userId;

    public ChatMemoryAdvisorEngine(MemGPTChatMemory chatMemory, ChatClient.Builder chatClientBuilder) {
        this.chatMemory = chatMemory;
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public void initialize(String userId) {

        this.userId = userId;

        chatClient = chatClientBuilder
                .defaultAdvisors(new MemGPTMessageChatMemoryAdvisor(chatMemory, userId))
                .build();

        System.out.println("Welcome.  Let's start a conversation.");
    }

    @Override
    public String chat(String message) {
        ChatResponse resp = chatClient.prompt().user(message).call().chatResponse();

        return resp.getResult().getOutput().getText();
    }

}
