package com.example.memgptagent.service;

import com.example.memgptagent.model.AgentCreate;
import com.example.memgptagent.model.AgentState;
import com.example.memgptagent.model.BlockCreate;
import com.example.memgptagent.BaseTest;
import com.example.memgptagent.repository.ToolRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Streamable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;

@SpringBootTest
public class MemGPTAgentTest extends BaseTest {

    private Agent createAgentInstance() {

        // get stock tools
        List<UUID> toolIds = Streamable.of(toolRepository.findAll()).stream().map(tool -> tool.getId()).collect(Collectors.toUnmodifiableList());

        BlockCreate humanBlock = new BlockCreate("human", "Person",5000, Map.of());
        BlockCreate personaBlock = new BlockCreate("personal", "Personal",5000, Map.of());

        AgentCreate agentCreate = new AgentCreate("Test", "Test Agent", "",
                toolIds, 16384, .75f, List.of(humanBlock, personaBlock), Map.of());

        AgentState agentState = agentManager.createAgent(agentCreate);

        return agentLoader.loadAgent(agentState.id(), chatClient ).get();
    }

}
