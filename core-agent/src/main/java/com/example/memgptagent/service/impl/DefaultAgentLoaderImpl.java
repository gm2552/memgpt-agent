package com.example.memgptagent.service.impl;

import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentLoader;
import com.example.memgptagent.service.AgentManager;
import com.example.memgptagent.service.AgentMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class DefaultAgentLoaderImpl implements AgentLoader {

    private final AgentManager agentManager;

    private final ObjectMapper objectMapper;

    private final AgentMetrics agentMetrics;

    public DefaultAgentLoaderImpl(AgentManager agentManager, ObjectMapper objectMapper, AgentMetrics agentMetrics) {
        this.agentManager = agentManager;
        this.objectMapper = objectMapper;
        this.agentMetrics = agentMetrics;
    }

    @Override
    public Optional<Agent> loadAgent(UUID agentId, ChatClient chatClient) {

        return agentManager.getAgentStateById(agentId).map(agentState -> {
            return Optional.of((Agent)new MemGPTAgent(agentState, agentManager, chatClient, objectMapper, agentMetrics));
        }).orElse(Optional.empty());

    }

    @Override
    public Optional<Agent> loadAgentByName(String agentName, ChatClient chatClient) {

        return agentManager.getAgentStateByName(agentName).map(agentState -> {
            return Optional.of((Agent)new MemGPTAgent(agentState, agentManager, chatClient, objectMapper, agentMetrics));
        }).orElse(Optional.empty());
    }
}
