package com.example.memgptagent.tool;

import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.function.Function;

public class ArchivalMemoryInsert implements Function<ArchivalMemoryInsert.ArchivalMemoryInsertRequest, ToolResponse> {

    private final Agent agent;

    private final AgentManager agentManager;

    public ArchivalMemoryInsert(Agent agent, AgentManager agentManager) {
        this.agentManager = agentManager;
        this.agent = agent;
    }

    @Override
    public ToolResponse apply(ArchivalMemoryInsertRequest archivalMemoryInsertRequest) {

        agentManager.insertPassage(agent.getId(), archivalMemoryInsertRequest.content);

        return ToolResponse.emptyOKStatus();
    }

    public record ArchivalMemoryInsertRequest(@JsonProperty(required = true)
                                      @JsonPropertyDescription("Deep inner monologue private to you only.") String innerThoughts,
                                      @JsonProperty(required = true)
                                      @JsonPropertyDescription("Content to write to the memory. All unicode (including emojis) are supported.") String content,
                                      @JsonProperty(required = true)
                                      @JsonPropertyDescription("Request an immediate heartbeat after function execution. Set to `True` if you want to send a follow-up message or run a follow-up function.") boolean requestHeartbeat) {}


}
