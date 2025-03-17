package com.java.example.chatbot.advisors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A MemMPT custom memory advisor that delegates to a MemGPT MCP service.  This allows for a virtually
 * infinite context window.  It encapsulates a custom ChatMemory implementation that handles the
 * communication with the underlying MemGPT service.
 * <br></>
 * Context memory is identified by the conversation ID; i.e. a new conversation ID will result in a
 * unique content memory.
 * <br></>
 * This advisor requires a previously created McpSyncClient that is configured to connect and
 * communicate with the MemGPT MCP service.
 *
 */
public class MemGPTMessageChatMemoryAdvisor extends MessageChatMemoryAdvisor  {

    public MemGPTMessageChatMemoryAdvisor(McpSyncClient client, String conversationId) {
        // Because MemGPT context windows are virtually and theoretically limitless,
        // a max number of messages to store is somewhat moot.
        // TODO: possibly control the number of previous messages that are returned
        // from the MemGPT service.  As of now, the MemGPT service can hold as many messages
        // as it's threshold limit allows, but this might be a different size than the model
        // that the ChatClient is targeting.
        super(new MemGPTChatMemory(client), conversationId, Integer.MAX_VALUE);
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {

        // we need the user message so the recallContext tool can examine it and
        // potentially perform operations such as modifying core memory or recalling
        // older conversations
        UserMessage userMessage = new UserMessage(advisedRequest.userText(), advisedRequest.media());

        ((MemGPTChatMemory)chatMemoryStore).setUserMessage(userMessage);

        return super.aroundCall(advisedRequest, chain);
    }

    /*
     * MemGPTChatMemory delegates to a MemGPT MCP service for recalling the relevant
     * context window and appending responses to the context store.
     */
    private static class MemGPTChatMemory implements ChatMemory {

        private static final Logger LOGGER = LoggerFactory.getLogger(MemGPTChatMemory.class);

        private static final String RECALL_TOOL_NAME = "recallContext";

        private static final String APPEND_TOOL_NAME = "appendContext";

        private static final String CLEAR_TOOL_NAME = "clear";

        private static final String AGENT_NAME_ARG = "agentName";

        private static final String PROMPT_MESSAGE_ARG = "promptMessage";

        private static final String MESSAGE_ARG = "messages";

        private final McpSyncClient client;

        private UserMessage userMessage;

        MemGPTChatMemory(McpSyncClient client) {
            this.client = client;
        }


        @Override
        public void add(String conversationId, List<Message> messages) {

            var args = Map.of(AGENT_NAME_ARG, conversationId, MESSAGE_ARG, messages);

            McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest(APPEND_TOOL_NAME, args);

            try{
                McpSchema.CallToolResult result = client.callTool(callToolRequest);

                if (result.isError())
                    LOGGER.warn("Error appending context.  Unknown error");
            }
            catch (Exception e) {
                LOGGER.warn("Error appending context: {}", e.getMessage());
            }

        }

        @Override
        public List<Message> get(String conversationId, int lastN) {

            // The user message is passes to update the context's core memory are to
            // retrieve any other relevant information from "virtual" memory.
            var args = Map.of(AGENT_NAME_ARG, conversationId, PROMPT_MESSAGE_ARG, userMessage);

            McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest(RECALL_TOOL_NAME, args);

            try{

                McpSchema.CallToolResult result = client.callTool(callToolRequest);

                if (result.isError())
                    LOGGER.warn("Error recalling context.  Unknown error");
                else {

                    if (!result.content().isEmpty()) {

                        // there should be one and only one content entry if there was not an error
                        String content = ((McpSchema.TextContent)result.content().getFirst()).text();

                        // The returned content is a list of JSON objects.  Each object represents
                        // an individual message which is in the form a Map<String, Object>
                        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        List<Object> retObjects = objectMapper.readValue(content, new TypeReference<List<Object>>(){});

                        return retObjects.stream().map(ob -> {

                            Map<String, Object> map = (Map<String, Object>)ob;

                            try {
                                // find the message type
                                MessageType type = MessageType.valueOf(map.get("messageType").toString());

                                // SpringAI Message type cannot be directly deserialized (as of writing) with the
                                // ObjectMapper.  The xxxWrapper classes facilitate the deserialization of each
                                // message type.
                                Class<? extends Message> messageClassType = switch(type) {
                                    case SYSTEM -> SystemWrapper.class;
                                    case TOOL -> ToolResponseWrapper.class;
                                    case ASSISTANT -> AssistantWrapper.class;
                                    case USER -> UserWrapper.class;
                                };

                                // deserialize the message
                                return (Message)objectMapper.readValue(objectMapper.writeValueAsString(ob), messageClassType);

                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }).toList();

                    }
                }

            }
            catch (Exception e) {
                LOGGER.warn("Error recalling context: {}", e.getMessage());

            }

            return List.of();

        }

        @Override
        public void clear(String conversationId) {
            var args = Map.of(AGENT_NAME_ARG, (Object)conversationId);

            McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest(CLEAR_TOOL_NAME, args);

            try{
                McpSchema.CallToolResult result = client.callTool(callToolRequest);

                if (result.isError())
                    LOGGER.warn("Error clearing context memory.  Unknown error");
            }
            catch (Exception e) {
                LOGGER.warn("Error clearing context memory: {}", e.getMessage());
            }
        }

        public void setUserMessage(UserMessage userMessage) {
            this.userMessage = userMessage;
        }

    }

    /////////////////////////////////////////
    //
    //
    // Wrapper classes for message deserialization
    //

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
        public SystemWrapper(@JsonProperty("text") String textContent) {
            super(textContent);
        }
    }

}


