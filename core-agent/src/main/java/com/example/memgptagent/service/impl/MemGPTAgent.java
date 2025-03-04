package com.example.memgptagent.service.impl;

import com.example.memgptagent.model.AgentState;
import com.example.memgptagent.model.Message;
import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentManager;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MemGPTAgent implements Agent {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemGPTAgent.class);

    private static final float DEFAULT_MEMORY_THRESHOLD = .75f;

    private AgentState agentState;

    private final ChatClient chatClient;

    private final AgentManager agentManager;

    private final ObjectMapper objectMapper;

    private String finalUserMessage;

    private float memoryThreshold;

    public MemGPTAgent(AgentState agentState, AgentManager agentManager, ChatClient chatClient,
                       ObjectMapper objectMapper) {

        this.agentState = agentState;
        this.chatClient = chatClient;
        this.agentManager = agentManager;
        this.objectMapper = objectMapper;
        this.finalUserMessage = "";
        this.memoryThreshold = DEFAULT_MEMORY_THRESHOLD;
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
    public OpenAiApi.ChatCompletion chat(OpenAiApi.ChatCompletionRequest chatRequest) {
        try {

            LOGGER.info("Chat request invoked for agent {}", agentState.name());

            return doInner(chatRequest);
        }
        catch (Exception e) {

            OpenAiApi.ChatCompletionMessage completionMessage =
                    new OpenAiApi.ChatCompletionMessage("Chat Error: " + e.getMessage(), OpenAiApi.ChatCompletionMessage.Role.USER);
            var choice = new OpenAiApi.ChatCompletion.Choice(OpenAiApi.ChatCompletionFinishReason.STOP,
                    0,  completionMessage, null);
            return new OpenAiApi.ChatCompletion(UUID.randomUUID().toString(), List.of(choice), System.currentTimeMillis(), chatRequest.model(),
                    "", "", "", null);
        }
    }



    /*
     * not currently implemented
     */
    @Override
    public Flux<OpenAiApi.ChatCompletionChunk> streamChat(OpenAiApi.ChatCompletionRequest chatRequest) {
        return null;
    }

    private OpenAiApi.ChatCompletion  doInner(OpenAiApi.ChatCompletionRequest chatRequest) {

        String model = chatRequest.model();

        // load messages in context
        List<org.springframework.ai.chat.messages.Message> contextMessages
                = toSpringAIMessages(agentManager.getMessagesByIds(agentState.messageIds()));

        // add the system message
        contextMessages.addFirst(buildSystemMessage());

        UserMessage userMsg = compileUserMessage(chatRequest);
        agentManager.saveNewMessages(this.getId(), toStorageMessage(List.of(userMsg)));

        contextMessages.add(compileUserMessage(chatRequest));

        OpenAiApi.Usage usage = runRequestLoop(contextMessages);

        // now refresh my state
        refreshState();

        LOGGER.info("Successfully completed chat transaction.  Current context size for agent {}: {}", agentState.name(), usage.promptTokens());

        if (usage.promptTokens() >  memoryThreshold * agentState.contextWindowSize())
            summarizeMessages();

        OpenAiApi.ChatCompletionMessage completionMessage =
                new OpenAiApi.ChatCompletionMessage(this.finalUserMessage, OpenAiApi.ChatCompletionMessage.Role.USER);
        var choice = new OpenAiApi.ChatCompletion.Choice(OpenAiApi.ChatCompletionFinishReason.STOP,
                0,  completionMessage, null);
        return new OpenAiApi.ChatCompletion(UUID.randomUUID().toString(), List.of(choice), System.currentTimeMillis(), model,
                "", "", "", usage);
    }

    private OpenAiApi.Usage runRequestLoop(List<org.springframework.ai.chat.messages.Message> contextMessages) {

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
                agentManager.saveNewMessages(this.getId(), toStorageMessage(List.of(lastMsg)));

                if (lastMsg.getMessageType() == MessageType.TOOL){
                    ToolResponseMessage toolMessage = (ToolResponseMessage) lastMsg;

                    for (ToolResponseMessage.ToolResponse resp :  toolMessage.getResponses())
                    {

                        // TODO: use a configured list of tool names to kick out of the loop
                        // check if the tool kicks us out of the loop
                        if (resp.name().equalsIgnoreCase("send_message")){
                            runLoop = false;
                            break;
                        }
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
        //.filter(msg -> msg.role() == MessageType.USER || msg.role() == MessageType.SYSTEM)
        .map(msg -> switch (msg.role()) {

            case USER -> new UserMessage(msg.content());
            case ASSISTANT -> new AssistantMessage(msg.content(), Map.of(), msg.toolCalls());
            case SYSTEM -> new SystemMessage(msg.content());
            case TOOL ->  toSpringToolResponseMessage(msg);

        }).collect(Collectors.toList());
    }

    private ToolResponseMessage toSpringToolResponseMessage(Message contextMessage) {

        List<ToolResponseMessage.ToolResponse> resps =
            Arrays.stream(contextMessage.toolCallId().split(",")).map(id -> new ToolResponseMessage.ToolResponse(id, "", "")).toList();

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
        }).collect(Collectors.toUnmodifiableList());

    }

    private Message assistantMessageToStorageMessage(AssistantMessage astMessage) {
            List<AssistantMessage.ToolCall> toolCalls = astMessage.hasToolCalls() ? astMessage.getToolCalls() : List.of();

        return new Message(null, MessageType.ASSISTANT, astMessage.getText(), toolCalls, "","");
    }

    private Message toolMessageToStorageMessage(ToolResponseMessage toolMessage) {

        StringBuilder builder = new StringBuilder();
        Iterator<ToolResponseMessage.ToolResponse> iter = toolMessage.getResponses().iterator();
        while (iter.hasNext()) {
            builder.append(iter.next().id());
            if (iter.hasNext())
                builder.append(",");
        }


        return new Message(null, MessageType.TOOL, toolMessage.getText(), List.of(), builder.toString(),"");
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
                return null;
            }
        }).toList();

        return toolCallbacks.toArray(new FunctionCallback[0]);

    }

    private SystemMessage buildSystemMessage() {

        StringBuilder builder = new StringBuilder(DefaultAgentContet.DEFAULT_SYSTEM_PROMPT)
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

        // create summary content and the the user message
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

}
