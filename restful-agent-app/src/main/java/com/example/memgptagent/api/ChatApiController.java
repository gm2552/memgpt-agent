package com.example.memgptagent.api;

import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatApiController {

    private final AgentLoader agentLoader;

    private final ChatClient.Builder chatClientBuilder;

    public ChatApiController(AgentLoader agentLoader, ChatClient.Builder chatClientBuilder) {
        this.agentLoader = agentLoader;
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostMapping(value="chat/{agentName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> chat(@PathVariable("agentName")String  agentName, @RequestBody OpenAiApi.ChatCompletionRequest chatRequest) {

        Agent agent = agentLoader.loadAgentByName(agentName, chatClientBuilder.build()).get();

        if (agent == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(agent.chat(chatRequest).getText());

    }
}
