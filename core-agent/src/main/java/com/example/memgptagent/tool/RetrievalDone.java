package com.example.memgptagent.tool;

import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentManager;
import com.example.memgptagent.service.MutableAgent;

import java.util.function.Supplier;

public class RetrievalDone implements Supplier<ToolResponse> {

    private final MutableAgent agent;

    private final AgentManager agentManager;

    public RetrievalDone(Agent agent, AgentManager agentManager) {
        this.agentManager = agentManager;
        this.agent = (MutableAgent)agent;
    }

    @Override
    public ToolResponse get() {
        return ToolResponse.emptyOKStatus();
    }

}
