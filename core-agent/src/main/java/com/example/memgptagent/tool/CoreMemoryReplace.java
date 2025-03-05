package com.example.memgptagent.tool;

import com.example.memgptagent.model.Block;
import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentManager;
import com.example.memgptagent.service.MutableAgent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public class CoreMemoryReplace implements Function<CoreMemoryReplace.MemoryReplaceRequest, ToolResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreMemoryReplace.class);

    private final MutableAgent agent;

    private final AgentManager agentManager;

    public CoreMemoryReplace(Agent agent, AgentManager agentManager) {
        this.agentManager = agentManager;
        this.agent = (MutableAgent)agent;
    }

    @Override
    public ToolResponse apply(MemoryReplaceRequest memoryReplaceRequest) {

        LOGGER.debug("Core memory replace initiated for agent {} for label {}", agent.getName(), memoryReplaceRequest.label());

        return agentManager.getAgentStateById(agent.getId()).map(state -> {

            Block block = state.memory().blocks().get(memoryReplaceRequest.label);
            if (block != null) {

                if (!StringUtils.hasText(block.value())){
                    agentManager.updateMemoryBlockValue(block.id(), memoryReplaceRequest.newContent);
                    agent.refreshState();
                }
                else if (block.value().indexOf(memoryReplaceRequest.oldContent) >= 0) {
                    String newBlockValue = block.value().replace(memoryReplaceRequest.oldContent, memoryReplaceRequest.newContent);
                    agentManager.updateMemoryBlockValue(block.id(), newBlockValue);
                    agent.refreshState();
                }
            }

            return ToolResponse.emptyOKStatus();

        }).orElse(new ToolResponse("FAIL", "Unknown block label: " + memoryReplaceRequest.label,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now())));
    }

    public record MemoryReplaceRequest(@JsonProperty(required = true)
                                       @JsonPropertyDescription("Deep inner monologue private to you only.") String innerThoughts,
                                       @JsonProperty(required = true)
                                       @JsonPropertyDescription("Section of the memory to be edited (persona or human).") String label,
                                       @JsonProperty(required = true)
                                       @JsonPropertyDescription("String to replace. Must be an exact match.") String oldContent,
                                       @JsonProperty(required = true)
                                       @JsonPropertyDescription("Content to write to the memory. All unicode (including emojis) are supported.") String newContent,
                                       @JsonProperty(required = true)
                                       @JsonPropertyDescription("Request an immediate heartbeat after function execution. Set to `True` if you want to send a follow-up message or run a follow-up function.") boolean requestHeartbeat) {}


}
