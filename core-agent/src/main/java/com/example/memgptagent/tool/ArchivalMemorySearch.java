package com.example.memgptagent.tool;

import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;
import java.util.function.Function;

public class ArchivalMemorySearch implements Function<ArchivalMemorySearch.ArchivalMemorySearchRequest, ToolResponse> {

    private final Agent agent;

    private final AgentManager agentManager;

    public ArchivalMemorySearch(Agent agent, AgentManager agentManager) {
        this.agentManager = agentManager;
        this.agent = agent;
    }

    @Override
    public ToolResponse apply(ArchivalMemorySearchRequest archivalMemorySearchRequest) {

        // for now, just a simple list of content with the passage and return using List.toString();

        List<String> passages = agentManager.getMatchingUserPassages(agent.getId(), archivalMemorySearchRequest.query)
                .stream().map(passage -> "content: " + passage).toList();

        if (passages.isEmpty()){
            ToolResponse.contentOKStatus("No results found.");
        }

        return ToolResponse.contentOKStatus(passages.toString());

    }

    public record ArchivalMemorySearchRequest(@JsonProperty(required = true)
                                            @JsonPropertyDescription("Deep inner monologue private to you only.") String innerThoughts,
                                              @JsonProperty(required = true)
                                            @JsonPropertyDescription("String to search for") String query,
                                              @JsonPropertyDescription("Allows you to page through results. Only use on a follow-up query. Defaults to 0 (first page).") Integer page,
                                              @JsonPropertyDescription("Starting index for the search results. Defaults to 0.")  Integer start,
                                              @JsonProperty(required = true)
                                            @JsonPropertyDescription("Request an immediate heartbeat after function execution. Set to `True` if you want to send a follow-up message or run a follow-up function.") boolean requestHeartbeat) {}
}
