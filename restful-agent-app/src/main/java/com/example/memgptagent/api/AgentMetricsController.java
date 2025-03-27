package com.example.memgptagent.api;

import com.example.memgptagent.service.AgentMetrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentMetricsController {
    private final AgentMetrics agentMetrics;

    public AgentMetricsController(AgentMetrics agentMetrics) {
        this.agentMetrics = agentMetrics;
    }

    @GetMapping("/metrics/{agentName}")
    public Metrics metrics(@PathVariable("agentName") String agentName) {
        Integer contextSize = agentMetrics.getContextSize(agentName);
        String human = agentMetrics.getHumanBlockValue(agentName);
        String persona = agentMetrics.getPersonaBlockValue(agentName);

        return new Metrics(contextSize, human, persona);
    }

    public record Metrics(Integer contextSize, String humanBlockValue, String personaBlockValue) {}
}
