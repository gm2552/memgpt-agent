package com.example.memgptagent.mcp;

import com.example.memgptagent.model.AgentCreate;
import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentLoader;
import com.example.memgptagent.service.AgentManager;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class MemGPTTools {

    private final AgentLoader agentLoader;

    private final AgentManager agentManager;

    private final ChatClient.Builder chatClientBuilder;

    private final int contextWindowSize;

    private final float summaryThreshold;

    private final ObjectMapper objectMapper;

    public MemGPTTools(ObjectMapper mapper, AgentLoader agentLoader, AgentManager agentManager, ChatClient.Builder chatClientBuilder,
                       @Value("${memgpt.agent.contextWindowsSize:16384}") int contextWindowSize,
                       @Value("${memgpt.agent.contextWindowsSummaryThreshold:0.75}") float summaryThreshold) {

        this.objectMapper = mapper;
        this.agentLoader = agentLoader;
        this.agentManager = agentManager;
        this.chatClientBuilder = chatClientBuilder;
        this.contextWindowSize = contextWindowSize;
        this.summaryThreshold = summaryThreshold;
    }

    @Tool(name="chat", description="Executes a continued chat given a user provided prompt and the system " +
            " returns a response to the continues chat.  The chat context is specified by the agent name")
    public String chat(@ToolParam(description="The unique name of the agent that maintains chat context and history") String agentName,
                       @ToolParam(description="The users prompt message that continues the conversation.")String promptMessage) {

        if (StringUtils.isEmpty(agentName))
            return "Invalid agentId.  AgentId cannot be empty.";

        if (StringUtils.isEmpty(promptMessage))
            return "Invalid promptMessage. PromptMessage cannot be empty.";

        Agent agent = getAgent(agentName);

        OpenAiApi.ChatCompletionMessage msg =
                new OpenAiApi.ChatCompletionMessage(promptMessage, OpenAiApi.ChatCompletionMessage.Role.USER);

        OpenAiApi.ChatCompletionRequest req = new OpenAiApi.ChatCompletionRequest(List.of(msg), "", .5);

        String result = agent.chat(req).getText();

        return result;

    }

    @Tool(name="recallContext", description="Retrieves the current relevant context and optionally updates the context based on the prompt message." +
            "The prompt message is optional if one is provided in the UserMessage.  The chat context is specified by the agent name.")
    public List<Message> recallContext(@ToolParam(description="The unique name of the agent that maintains chat context and history") String agentName,
                              @ToolParam(description="The users prompt message that continues the conversation.", required=false) String promptMessage) {

        if (StringUtils.isEmpty(agentName))
            return List.of();

        Agent agent = getAgent(agentName);

        OpenAiApi.ChatCompletionMessage msg =
                new OpenAiApi.ChatCompletionMessage(promptMessage, OpenAiApi.ChatCompletionMessage.Role.USER);

        OpenAiApi.ChatCompletionRequest req = new OpenAiApi.ChatCompletionRequest(List.of(msg), "", .5);

        return agent.recallContext(StringUtils.hasText(promptMessage) ? new UserMessage(promptMessage) : null);
    }


    @Tool(name="appendContext", description="Appends the context with updated messages." +
            "The chat context is specified by the agent name.")
    public void appendContext(@ToolParam(description="The unique name of the agent that maintains chat context and history") String agentName,
                              @ToolParam(description="The completion messages to add to the context.") List<HashMap> messages) {


        List<Message> msgs = messages.stream().filter(msg -> msg.get("messageType") != null)
            .map(msg -> {

                MessageType msgType = MessageType.valueOf((String) msg.get("messageType"));

                Class<? extends Message> messageClassType = switch(msgType) {
                    case SYSTEM -> SystemWrapper.class;
                    case TOOL -> ToolResponseWrapper.class;
                    case ASSISTANT -> AssistantWrapper.class;
                    case USER -> UserWrapper.class;
                };

                try {
                    return (Message)objectMapper.readValue(objectMapper.writeValueAsString(msg), messageClassType);
                } catch (JsonProcessingException e) {
                    // TODO Handle this better
                    return null;
                }
            }).collect(Collectors.toUnmodifiableList());


        if (StringUtils.hasText(agentName)) {

            Agent agent = getAgent(agentName);

            agent.appendContext(msgs);

        }

    }

    private Agent getAgent(String agentName) {

        // load the agent and see check if the agent exists
        Optional<Agent> agent = agentLoader.loadAgentByName(agentName, chatClientBuilder.build());

        if (agent.isEmpty()) {

            AgentCreate agentCreate = new AgentCreate(agentName, "", "",
                    List.of(), contextWindowSize, summaryThreshold, List.of(), Map.of());

            // need to create a new agent/session
            agentManager.createAgent(agentCreate);

            // now load the agent again
            agent = agentLoader.loadAgentByName(agentName, chatClientBuilder.build());
        }

        return agent.get();

    }

    private static class ToolResponseWrapper extends ToolResponseMessage {

        @JsonCreator
        public ToolResponseWrapper(@JsonProperty("responses") List<ToolResponse> responses, @JsonProperty("metadata") Map<String, Object> metadata) {

            super(responses, metadata);
        }
    }

    private static class AssistantWrapper extends AssistantMessage {

        @JsonCreator
        public AssistantWrapper(@JsonProperty("text") String content, @JsonProperty("metadata") Map<String, Object> properties, @JsonProperty("toolCalls") List<ToolCall> toolCalls, @JsonProperty("media") List<Media> media) {

            super(content, properties, toolCalls, media);
        }
    }

    private static class UserWrapper extends UserMessage {

        @JsonCreator
        public UserWrapper(@JsonProperty("messageType") MessageType messageType, @JsonProperty("text") String textContent, @JsonProperty("media") Collection<Media> media, @JsonProperty("metadata") Map<String, Object> metadata) {
            super(messageType, textContent, media, metadata);
        }
    }

    private static class SystemWrapper extends SystemMessage {

        @JsonCreator
        public SystemWrapper(@JsonProperty("text")  String textContent) {
            super(textContent);
        }
    }

}
