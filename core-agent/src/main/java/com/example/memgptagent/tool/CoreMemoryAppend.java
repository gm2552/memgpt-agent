package com.example.memgptagent.tool;

import com.example.memgptagent.model.Block;
import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public class CoreMemoryAppend implements Function<CoreMemoryAppend.MemoryAppendRequest, ToolResponse> {

    private final Agent agent;

    private final AgentManager agentManager;

    public CoreMemoryAppend(Agent agent, AgentManager agentManager) {
        this.agentManager = agentManager;
        this.agent = agent;
    }

    @Override
    public ToolResponse apply(MemoryAppendRequest memoryAppendRequest) {

        return agentManager.getAgentStateById(agent.getId()).map(state -> {

            Block block = state.memory().blocks().get(memoryAppendRequest.label);
            if (block != null) {

                String prevValue = StringUtils.hasText(block.value()) ? block.value() + "\n" : "";

                String newBlockValue = prevValue + memoryAppendRequest.content;

                agentManager.updateMemoryBlockValue(block.id(), newBlockValue);

                agent.refreshState();
            }

            return ToolResponse.emptyOKStatus();

        }).orElse(new ToolResponse("FAIL", "Unknown block label: " + memoryAppendRequest.label,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now())));
    }

    public record MemoryAppendRequest(@JsonProperty(required = true)
                                      @JsonPropertyDescription("Deep inner monologue private to you only.") String innerThoughts,
                                      @JsonProperty(required = true)
                                      @JsonPropertyDescription("Section of the memory to be edited (persona or human).") String label,
                                      @JsonProperty(required = true)
                                      @JsonPropertyDescription("Content to write to the memory. All unicode (including emojis) are supported.") String content,
                                      @JsonProperty(required = true)
                                      @JsonPropertyDescription("Request an immediate heartbeat after function execution. Set to `True` if you want to send a follow-up message or run a follow-up function.") boolean requestHeartbeat) {}
}
