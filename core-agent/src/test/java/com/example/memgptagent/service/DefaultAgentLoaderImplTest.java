package com.example.memgptagent.service;

import com.example.memgptagent.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class DefaultAgentLoaderImplTest extends BaseTest {

    @Test
    public void testCreateLoadAgentById() {

        UUID agentId = createNewAgent();

        Optional<Agent> agent = agentLoader.loadAgent(agentId, mock(ChatClient.class));

        assertNotNull(agent);
    }

}
