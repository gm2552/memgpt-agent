package com.example.memgptagent.api;

import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentLoader;
import com.example.memgptagent.service.AgentManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ContextApiController {

    private final AgentLoader agentLoader;

    private final AgentManager agentManager;

    private final ChatClient.Builder chatClientBuilder;

    public ContextApiController(AgentLoader agentLoader, AgentManager agentManager, ChatClient.Builder chatClientBuilder) {
        this.agentLoader = agentLoader;
        this.agentManager = agentManager;
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostMapping(value="recallContext/{agentName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Message>> recallContext(@PathVariable("agentName")String  agentName, @RequestBody String promptMessage) {

        Agent agent = agentLoader.loadAgentByName(agentName, chatClientBuilder.build()).get();

        if (agent == null)
            return ResponseEntity.notFound().build();

        OpenAiApi.ChatCompletionMessage msg =
                new OpenAiApi.ChatCompletionMessage(promptMessage, OpenAiApi.ChatCompletionMessage.Role.USER);

        OpenAiApi.ChatCompletionRequest req = new OpenAiApi.ChatCompletionRequest(List.of(msg), "", .5);

        return ResponseEntity.ok(agent.recallContext(StringUtils.hasText(promptMessage) ? new UserMessage(promptMessage) : null));
    }

    @PostMapping(value="appendContext/{agentName}")
    public ResponseEntity<Void> appendContext(@PathVariable("agentName")String  agentName, @RequestBody String messagesContent) throws JsonProcessingException {

        Agent agent = agentLoader.loadAgentByName(agentName, chatClientBuilder.build()).get();

        if (agent == null)
            return ResponseEntity.notFound().build();

        agent.appendContext(MessageUtils.deserializeMessagesFromJSONString(messagesContent));

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value="clearContext/{agentName}")
    public ResponseEntity<Void> clearContext(@PathVariable("agentName")String agentName) throws JsonProcessingException {

        agentManager.clearAgentStateByName(agentName);

        return ResponseEntity.ok().build();
    }


}
