package com.example.memgptagent.api;

import com.example.memgptagent.model.AgentCreate;
import com.example.memgptagent.model.AgentState;
import com.example.memgptagent.repository.ToolRepository;
import com.example.memgptagent.service.AgentManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
public class AgentApiController {

    private final AgentManager agentManager;

    private final ToolRepository toolRepository;

    public AgentApiController(AgentManager agentManager, ToolRepository toolRepository) {
        this.agentManager = agentManager;
        this.toolRepository = toolRepository;
    }


    @PostMapping("agent")
    public ResponseEntity<AgentState> createAgent(@RequestBody AgentCreateRequest agentCreateRequest) {

        if (agentManager.getAgentStateByName(agentCreateRequest.agentName).isPresent()) {
            return ResponseEntity.status(409).build();
        }

        return ResponseEntity.ok(createNewAgent(agentCreateRequest));
    }

    private AgentState createNewAgent(AgentCreateRequest agentCreateRequest) {

        AgentCreate agentCreate = new AgentCreate(agentCreateRequest.agentName, "", "",
                List.of(), agentCreateRequest.contextWindowSize, .75f, List.of(), Map.of());

        return agentManager.createAgent(agentCreate);
    }

    public record AgentCreateRequest(String agentName, int contextWindowSize) {}
}
