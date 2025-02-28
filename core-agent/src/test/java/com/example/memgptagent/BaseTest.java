package com.example.memgptagent;

import com.example.memgptagent.entity.Agent;
import com.example.memgptagent.entity.Block;
import com.example.memgptagent.entity.Tool;
import com.example.memgptagent.repository.AgentRepository;
import com.example.memgptagent.repository.BlockRepository;
import com.example.memgptagent.repository.ToolRepository;
import com.example.memgptagent.service.AgentLoader;
import com.example.memgptagent.service.AgentManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
public class BaseTest {

    @Autowired
    protected AgentManager agentManager;

    @Autowired
    protected AgentLoader agentLoader;

    @Autowired
    protected AgentRepository agentRepository;

    @Autowired
    protected BlockRepository blockRepository;

    @Autowired
    protected ToolRepository toolRepository;

    @Autowired
    protected ChatClient chatClient;

    protected UUID createNewAgent() {

        Tool tool = new Tool();
        tool.setDescription("Test tool");
        tool.setName("Test tool");
        tool.setReturnCharacterLimit(1000);
        tool.setFqClassName("java.util.Map");
        tool.setFqInputClassName("java.util.List");

        UUID toolId = toolRepository.save(tool).getId();

        com.example.memgptagent.entity.Agent agent = new Agent();
        agent.setAgentName("test");
        agent.setContextWindow(4096);
        agent.setDescription("test description");
        agent.setMessageIds(List.of());
        agent.setToolIds(List.of(tool.getId()));
        agent.setMetadataLabels(Map.of());
        agent.setSystemPrompt("The system prompt");
        agent.setVersion(0);


        UUID agentId = agentRepository.save(agent).getId();

        Block block = new Block();
        block.setAgentId(agentId);
        block.setDescription("test human description");
        block.setLabel("human");
        block.setMetadata(Map.of());
        block.setValue("Name: Jim");

        blockRepository.save(block);

        block = new Block();
        block.setAgentId(agentId);
        block.setDescription("test human description");
        block.setLabel("agent");
        block.setMetadata(Map.of());
        block.setValue("Persona: Funny");

        blockRepository.save(block);

        return agentId;
    }

}
