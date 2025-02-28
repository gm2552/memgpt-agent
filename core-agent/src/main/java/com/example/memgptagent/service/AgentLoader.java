package com.example.memgptagent.service;

import org.springframework.ai.chat.client.ChatClient;

import java.util.Optional;
import java.util.UUID;

public interface AgentLoader {

    Optional<Agent> loadAgent(UUID agentId, ChatClient chatClient);

    Optional<Agent> loadAgentByName(String agentName, ChatClient chatClient);
}
