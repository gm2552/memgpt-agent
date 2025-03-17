package com.java.example.chatbot.advisors;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;

public interface MemGPTChatMemory extends ChatMemory {

    void setUserMessage(UserMessage userMessage);
}
