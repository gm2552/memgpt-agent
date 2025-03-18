package com.example.memgptagent.api;

import com.example.memgptagent.model.AgentCreate;
import com.example.memgptagent.model.AgentState;
import com.example.memgptagent.repository.ToolRepository;
import com.example.memgptagent.service.AgentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
public class AgentApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentApiController.class);

    private final AgentManager agentManager;

    private final ToolRepository toolRepository;

    public AgentApiController(AgentManager agentManager, ToolRepository toolRepository) {
        this.agentManager = agentManager;
        this.toolRepository = toolRepository;
    }


    @PostMapping("agent")
    public ResponseEntity<AgentState> createAgent(@RequestBody AgentCreateRequest agentCreateRequest) {

        LOGGER.info("Requesting creation of agent {}", agentCreateRequest.agentName);

        if (agentManager.getAgentStateByName(agentCreateRequest.agentName).isPresent()) {
            LOGGER.warn("Agent {} already exists.", agentCreateRequest.agentName);
            return ResponseEntity.status(409).build();
        }

        return ResponseEntity.ok(createNewAgent(agentCreateRequest));
    }

    @DeleteMapping("agent/{agentName}/context")
    public ResponseEntity<Void> clearAgentContextMemory(@PathVariable("agentName")String  agentName) {

        LOGGER.info("Clearing memory for agent {}", agentName);

        agentManager.clearAgentStateByName(agentName);

        return ResponseEntity.ok().build();
    }

    private AgentState createNewAgent(AgentCreateRequest agentCreateRequest) {

        AgentCreate agentCreate = new AgentCreate(agentCreateRequest.agentName, "", "",
                List.of(), agentCreateRequest.contextWindowSize, .75f, List.of(), Map.of());

        return agentManager.createAgent(agentCreate);
    }

    public record AgentCreateRequest(String agentName, int contextWindowSize) {}
}
