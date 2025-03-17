package com.example.memgptagent.service.impl;

import com.example.memgptagent.model.AgentState;
import com.example.memgptagent.model.Message;
import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentManager;
import com.example.memgptagent.service.MutableAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;

import javax.naming.OperationNotSupportedException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MemGPTAgent implements MutableAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemGPTAgent.class);

    private AgentState agentState;

    private final ChatClient chatClient;

    private final AgentManager agentManager;

    private final ObjectMapper objectMapper;

    private String finalUserMessage;

    public MemGPTAgent(AgentState agentState, AgentManager agentManager, ChatClient chatClient,
                       ObjectMapper objectMapper) {

        this.agentState = agentState;
        this.chatClient = chatClient;
        this.agentManager = agentManager;
        this.objectMapper = objectMapper;
        this.finalUserMessage = "";
    }

    @Override
    public UUID getId() {
        return agentState.id();
    }

    @Override
    public String getName() {
        return agentState.name();
    }

    @Override
    public void refreshState() {
        agentManager.getAgentStateById(agentState.id()).ifPresent(state -> {
            agentState = state;
        });
    }


    @Override
    public synchronized void setFinalUserMessage(String finalMessage) {
        this.finalUserMessage = finalMessage;
    }


    @Override
    public List<org.springframework.ai.chat.messages.Message> recallContext(UserMessage request) {

        List<org.springframework.ai.chat.messages.Message> retMessages =
                new ArrayList<>(doInner(request, InnerOperation.CONTEXT_RETRIEVAL));

        // need to build a short system message with the core memory contents
        retMessages.addFirst(buildContextRetrievalMemoryMessage());

        return retMessages;
    }

    @Override
    public void appendContext(List<org.springframework.ai.chat.messages.Message> messages) {

        try {

            var convertedMessages = toStorageMessage(messages);

            agentManager.saveNewMessages(this.getId(), toStorageMessage(messages));

            // TODO: Do we need to reevaluate the token size and this point
            // and potentially run an evaluation (it will get run on the next appropriate
            // context retrival if we don't run it now, but will the window size be too large)?

            refreshState();
        }
        catch (Exception e) {

            LOGGER.warn("Failed to append context: {}", e.getMessage());

        }

    }

    @Override
    public AssistantMessage chat(OpenAiApi.ChatCompletionRequest chatRequest) {
        try {

            LOGGER.info("Chat request invoked for agent {}", agentState.name());

            List<org.springframework.ai.chat.messages.Message> messages =
                    doInner(compileUserMessage(chatRequest), InnerOperation.COMPLETION);

            // error
            if (messages.isEmpty())
                return new AssistantMessage("Chat Error: No chat response was created");

            org.springframework.ai.chat.messages.Message message = messages.getLast();

            if (message instanceof AssistantMessage)
                return (AssistantMessage)message;

            // error
            return new AssistantMessage("Chat Error: No valid response message was created");

        }
        catch (Exception e) {

            return new AssistantMessage("Chat Error: " + e.getMessage());
        }
    }



    /*
     * not currently implemented
     */
    @Override
    public Flux<OpenAiApi.ChatCompletionChunk> streamChat(OpenAiApi.ChatCompletionRequest chatRequest) {
        throw new RuntimeException(new OperationNotSupportedException("Streaming is not supported"));
    }

    private List<org.springframework.ai.chat.messages.Message> doInner(UserMessage chatRequest,
                                              InnerOperation operation) {

        // load messages in context
        List<org.springframework.ai.chat.messages.Message> contextMessages
                = toSpringAIMessages(agentManager.getMessagesByIds(agentState.messageIds()));

        if (operation == InnerOperation.CONTEXT_RETRIEVAL) {
            // if there is not a chat request, just get the context messages
            // we aren't trying to update memory
            // with any information from the request
            if (chatRequest == null)
                return contextMessages;
            // otherwise add the context retrieval system message
            else
                contextMessages.addFirst(buildContextRetrievalSystemMessage());
        }
        else
            // add the completion system message
            contextMessages.addFirst(buildCompletionSystemMessage());

        // don't update the messages with the user messages if it's not a completion... this should be done on an append operation
        if (operation == InnerOperation.COMPLETION)
            agentManager.saveNewMessages(this.getId(), toStorageMessage(List.of(chatRequest)));

        contextMessages.add(chatRequest);

        OpenAiApi.Usage usage = runRequestLoop(contextMessages, operation);

        // now refresh my state
        refreshState();

        LOGGER.info("Successfully completed chat transaction.  Current context size for agent {}: {}", agentState.name(), usage.promptTokens());

        if (usage.promptTokens() >  agentState.summaryThreshold() * agentState.contextWindowSize())
            summarizeMessages();

        if (operation == InnerOperation.COMPLETION)
            // if this is a completion, then we just want to return the assistant message
            return List.of(new AssistantMessage(finalUserMessage));
        else if (operation == InnerOperation.CONTEXT_RETRIEVAL)
            // context retrival just wants the currently saved messages
            // but after we have potentially updated memory
            // we'll save messages on an append operations
            // state should be refreshed at this point, so we'll have all of the latest
            // context information that may have been updated
            return toSpringAIMessages(agentManager.getMessagesByIds(agentState.messageIds()));

        // should not happen, but fall back return value
        return List.of();

    }

    private OpenAiApi.Usage runRequestLoop(List<org.springframework.ai.chat.messages.Message> contextMessages,
                                           InnerOperation operation) {

        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;

        // TODO: Might need a better way to do this so we can have options
        // specific to the chat model.  Maybe this should be done with
        // autoconfiguration properties since each ChatModel has its own set
        // of configuration.
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false).temperature(0.8).toolCallbacks(getFunctionCallbacks()).build();


        Prompt prompt = new Prompt(contextMessages, options);

        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

        // everything is done via tools, so loop until we hit a tool that terminates the loop
        // or kick out if we get a normal response message
        boolean runLoop = true;
        while (runLoop) {

            boolean heartbeat = false;

            ChatResponse chatResp = chatClient.prompt(prompt).call().chatResponse();

            Usage usg = chatResp.getMetadata().getUsage();
            promptTokens = usg.getPromptTokens();
            completionTokens = usg.getCompletionTokens();
            totalTokens = usg.getTotalTokens();

            // don't update the messages with the user messages if it's not a completion... this should be done on an append operation
            if (operation == InnerOperation.COMPLETION)
                agentManager.saveNewMessages(this.getId(), toStorageMessage(List.of(chatResp.getResult().getOutput())));

            ToolExecutionResult toolExecutionResult = null;
            if (chatResp.hasToolCalls()) {

                try {
                    Map<String, String> args = objectMapper.readValue(chatResp.getResult().getOutput().getToolCalls().getFirst().arguments(),
                            new TypeReference<Map<String,String>>(){});

                    String hb = args.getOrDefault("requestHeartbeat", "false");
                    heartbeat = Boolean.parseBoolean(hb);
                } catch (JsonProcessingException e) {
                    // do nothing for now
                }

                toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResp);

                org.springframework.ai.chat.messages.Message lastMsg = toolExecutionResult.conversationHistory().getLast();

                // don't update the messages with the user messages if it's not a completion...
                // this should be done on an append operation
                if (operation == InnerOperation.COMPLETION)
                    agentManager.saveNewMessages(this.getId(), toStorageMessage(List.of(lastMsg)));


                if (lastMsg.getMessageType() == MessageType.TOOL){
                    ToolResponseMessage toolMessage = (ToolResponseMessage) lastMsg;

                    boolean saveContextRetrivalMessages = false;

                    for (ToolResponseMessage.ToolResponse resp :  toolMessage.getResponses())
                    {

                        // if the tool is an archival and conversation retrieval ANDs this is a context
                        // retrival request, then we still need to add the tool response to the message list
                        if (operation == InnerOperation.CONTEXT_RETRIEVAL & (
                                resp.name().equalsIgnoreCase("archival_memory_search") ||
                                resp.name().equalsIgnoreCase("conversation_search"))) {
                            saveContextRetrivalMessages = true;

                        }

                        // TODO: use a configured list of tool names to kick out of the loop
                        // check if the tool kicks us out of the loop
                        if (resp.name().equalsIgnoreCase("send_message") || resp.name().equalsIgnoreCase("retrieval_done")){
                            runLoop = false;
                        }
                    }

                    if (saveContextRetrivalMessages) {
                        agentManager.saveNewMessages(this.getId(), toStorageMessage(List.of(chatResp.getResult().getOutput())));
                        agentManager.saveNewMessages(this.getId(), toStorageMessage(List.of(lastMsg)));
                    }
                }
            }
            else // kick out
            {
                LOGGER.warn("LLM returned a non tool response for agent {}.  Setting last message to returned response.", agentState.name());
                this.setFinalUserMessage(chatResp.getResult().getOutput().getText());
                runLoop = false;
            }


            if (runLoop) {
                contextMessages = new ArrayList<>(toolExecutionResult.conversationHistory());

                // figure out the next message to send

                try {
                    if (heartbeat) {
                        contextMessages.add(new UserMessage(objectMapper.writeValueAsString(new HeartBeatMessage("", "", ""))));
                    }

                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                prompt = new Prompt(contextMessages, options);
            }

        }

        return new OpenAiApi.Usage(completionTokens, promptTokens, totalTokens);

    }

    private List<org.springframework.ai.chat.messages.Message> toSpringAIMessages(List<Message> contextMessages) {

        return contextMessages.stream()
        .map(msg -> switch (msg.role()) {

            case USER -> new UserMessage(msg.content());
            case ASSISTANT -> new AssistantMessage(msg.content(), Map.of(), msg.toolCalls());
            case SYSTEM -> new SystemMessage(msg.content());
            case TOOL ->  toSpringToolResponseMessage(msg);

        }).collect(Collectors.toList());
    }

    private ToolResponseMessage toSpringToolResponseMessage(Message contextMessage) {

        List<ToolResponseMessage.ToolResponse> resps = new ArrayList<>();

        String[] ids = contextMessage.toolCallId().split(",");
        String[] resp = contextMessage.content().split("#TOOL_REP#");

        if (ids.length != resp.length){
            LOGGER.warn("Tool response consistency issue.  Returning empty response test for tools");
            for (int i = 0; i < ids.length; i++)
                resps.add(new ToolResponseMessage.ToolResponse(ids[i], "", ""));
        }
        else {
            for (int i = 0; i < ids.length; i++)
                resps.add(new ToolResponseMessage.ToolResponse(ids[i], "", resp[i]));
        }

        return new ToolResponseMessage(resps);

    }

    private List<Message> toStorageMessage(List<org.springframework.ai.chat.messages.Message> msgs) {

        return msgs.stream().map(msg -> {
            return switch (msg.getMessageType()) {
                case SYSTEM -> new Message(null, MessageType.SYSTEM, msg.getText(), List.of(), "","");
                case USER -> new Message(null, MessageType.USER, msg.getText(), List.of(), "","");
                case ASSISTANT ->  assistantMessageToStorageMessage((AssistantMessage)msg);
                case TOOL -> toolMessageToStorageMessage((ToolResponseMessage)msg);

            };
        }).toList();

    }

    private Message assistantMessageToStorageMessage(AssistantMessage astMessage) {
            List<AssistantMessage.ToolCall> toolCalls = astMessage.hasToolCalls() ? astMessage.getToolCalls() : List.of();

        return new Message(null, MessageType.ASSISTANT, astMessage.getText(), toolCalls, "","");
    }

    private Message toolMessageToStorageMessage(ToolResponseMessage toolMessage) {

        StringBuilder idBuilder = new StringBuilder();
        StringBuilder toolTextBuilder = new StringBuilder();
        Iterator<ToolResponseMessage.ToolResponse> iter = toolMessage.getResponses().iterator();
        while (iter.hasNext()) {
            ToolResponseMessage.ToolResponse toolResponse = iter.next();
            toolTextBuilder.append(toolResponse.responseData());
            idBuilder.append(toolResponse.id());
            if (iter.hasNext()) {
                idBuilder.append(",");
                // TODO: Find better way to combine tool id and response text for
                // potential parallel tool calls.
                // Using the delimiter below is probably not the best idea
                toolTextBuilder.append("#TOOL_REP#");
            }
        }


        return new Message(null, MessageType.TOOL, toolTextBuilder.toString(), List.of(), idBuilder.toString(),"");
    }

    private FunctionCallback[] getFunctionCallbacks() {

        List<FunctionCallback> toolCallbacks = agentState.tools().stream().map(tool -> {
            FunctionCallback.Builder builder = FunctionCallback.builder();

            FunctionCallback.FunctionInvokingSpec fncSpec = null;

            try{

                Object toolOb = tool.toolClass().getConstructor(Agent.class, AgentManager.class).newInstance(this, agentManager);
                if (toolOb instanceof Supplier<?>)
                    fncSpec = builder.function(tool.name(), (Supplier)toolOb);
                else if (toolOb instanceof Function)
                    fncSpec = builder.function(tool.name(), (Function)toolOb);
                else if (toolOb instanceof Consumer)
                    fncSpec = builder.function(tool.name(), (Consumer)toolOb);
                else
                    fncSpec = builder.function(tool.name(), (BiFunction)toolOb);

                if (tool.inputTypeClass() != null)
                    fncSpec.inputType(tool.inputTypeClass());

                fncSpec.description(tool.description());
                return fncSpec.build();

            }
            catch (Exception e) {
                LOGGER.warn("Could not create FunctionCallback for tool {}", tool.name(), e);
                return null;
            }
        })
        .filter(Objects::nonNull)
        .toList();

        return toolCallbacks.toArray(new FunctionCallback[0]);

    }

    private SystemMessage buildCompletionSystemMessage() {

        StringBuilder builder = new StringBuilder(DefaultAgentContet.COMPLETION_SYSTEM_PROMPT)
                .append("\n").append(buildSystemMessageMemoryBlock());

        return new SystemMessage(builder.toString());

    }

    private SystemMessage buildContextRetrievalSystemMessage() {

        StringBuilder builder = new StringBuilder(DefaultAgentContet.CONTEXT_RETRIEVAL_SYSTEM_PROMPT)
                .append("\n").append(buildSystemMessageMemoryBlock());

        return new SystemMessage(builder.toString());
    }

    private SystemMessage buildContextRetrievalMemoryMessage() {

        StringBuilder builder = new StringBuilder(DefaultAgentContet.CONTEXT_RETRIEVAL_MEMORY_BLOCK_TEMPLATE)
                .append("\n").append(buildSystemMessageMemoryBlock());

        return new SystemMessage(builder.toString());
    }

    private String buildSystemMessageMemoryBlock() {

        String time = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());

        return String.format(DefaultAgentContet.SYSTEM_MEMORY_BLOCK_TEMPLATE, time, agentState.messageIds().size(),
                0, agentState.memory().getCompiledMemoryBlock());

    }

    private UserMessage compileUserMessage(OpenAiApi.ChatCompletionRequest request) {

        String time = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());

        CompiledUserMessage msg = new CompiledUserMessage("user_message",
                request.messages().getFirst().content(), time);

        try {

            String value = objectMapper.writeValueAsString(msg);

            return new UserMessage(objectMapper.writeValueAsString(msg));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean exceededContextSize(Exception e) {

        return true;
    }

    private void summarizeMessages() {

        LOGGER.info("Agent {} hit memory threshold.  Summarizing messages.", agentState.name());

        // load messages in context
        List<Message> contextMessages = agentManager.getMessagesByIds(agentState.messageIds());

        // create the system message
        SystemMessage sysMessage = new SystemMessage(DefaultAgentContet.SUMMARY_SYSTEM_PROMPT);

        // create the assistant message
        AssistantMessage assMessage = new AssistantMessage(DefaultAgentContet.SUMMARY_ASSISTANT_ACK);

        // create summary content and the user message
        final StringBuilder builder = new StringBuilder();
        contextMessages.stream().forEach(msg -> builder.append("\n").append(msg.role()).append(": ").append(msg.content()));

        UserMessage userMessage = new UserMessage(builder.toString());

        ChatResponse chatResp = chatClient.prompt().messages(sysMessage, assMessage, userMessage).call().chatResponse();

        // create the new summary content that will go in the message context
        String contextSummaryContent =
                String.format(DefaultAgentContet.IN_CONTEXT_SUMMARY_TEMPLATE, chatResp.getResult().getOutput().getText());

        Message summaryMessage = new Message(null, MessageType.USER, contextSummaryContent, List.of(), "","");

        // now update the agent with the summary
        agentManager.replaceContextWithNewMessages(agentState.id(), List.of(summaryMessage));

        // update the agent
        refreshState();
    }

    private record CompiledUserMessage(String type, String message, String time) {}

    private record HeartBeatMessage(String type, String reason, String time) {

        public HeartBeatMessage {
            type = "heartbeat";
            reason = "[This is an automated system message hidden from the user] Function called using request_heartbeat=true, returning control";
            time = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
        }
    }

    private enum InnerOperation {

        COMPLETION,

        CONTEXT_RETRIEVAL

    }

}
