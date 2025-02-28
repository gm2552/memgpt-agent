package com.example.memgptagent.repository;

import com.example.memgptagent.entity.Agent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;

@SpringBootTest
public class AgentRepositoryTest {

    @Autowired
    private AgentRepository agentRepository;

    @Test
    public void testSaveGetAgent() {

        Agent agent = new Agent();
        agent.setAgentName("test");
        agent.setContextWindow(4096);
        agent.setDescription("test description");
        agent.setMessageIds(List.of());
        agent.setToolIds(List.of());
        agent.setMetadataLabels(Map.of());
        agent.setSystemPrompt("The system prompt");
        agent.setVersion(0);

        UUID agentId = agentRepository.save(agent).getId();

        Agent savedAgent = agentRepository.findById(agentId).get();

        assertNotNull(savedAgent);

    }
}
