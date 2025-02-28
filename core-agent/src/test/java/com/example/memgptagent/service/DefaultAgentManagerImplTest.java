package com.example.memgptagent.service;

import com.example.memgptagent.BaseTest;
import com.example.memgptagent.model.AgentState;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class DefaultAgentManagerImplTest extends BaseTest {

    @Test
    public void testGetAgentStateById() {

        UUID agentId = createNewAgent();

        Optional<AgentState> agentStateOp = agentManager.getAgentStateById(agentId);

        assertTrue(agentStateOp.isPresent());
    }
}
