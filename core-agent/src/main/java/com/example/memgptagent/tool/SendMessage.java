package com.example.memgptagent.tool;

import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.function.Consumer;

public class SendMessage implements Consumer<SendMessage.SendMessageRequest> {

    private final Agent agent;

    private final AgentManager agentManager;

    public SendMessage(Agent agent, AgentManager agentManager) {
        this.agentManager = agentManager;
        this.agent = agent;
    }

    @Override
    public void accept(SendMessageRequest sendMessageRequest) {

        agent.setFinalUserMessage(sendMessageRequest.message());
    }

    public record SendMessageRequest(@JsonProperty(required = true)
                                     @JsonPropertyDescription("Deep inner monologue private to you only.") String innerThoughts,
                                     @JsonProperty(required = true)
                                     @JsonPropertyDescription("Message contents. All unicode (including emojis) are supported.") String message) {};
}
