package com.example.memgptagent.service.impl;

import com.example.memgptagent.service.AgentMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultAgentMetrics implements AgentMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAgentMetrics.class);

    private final Map<String,Integer> contextSizes = new ConcurrentHashMap<>();
    private final Map<String,String> humanBlockValues = new ConcurrentHashMap<>();
    private final Map<String,String> personaBlockValues = new ConcurrentHashMap<>();

    @Override
    public Integer getContextSize(String agentName) {
        return contextSizes.get(agentName);
    }

    @Override
    public void setContextSize(String agentName, Integer integer) {
        LOGGER.info("setContextSize: agentName={}, size={}", agentName, integer);
        contextSizes.put(agentName,integer);
    }

    @Override
    public void setCoreMemory(String agentName, String humanBlockValue, String personaBlockValue) {
        LOGGER.info("setCoreMemory agentName={}, human={}, persona={}", agentName, humanBlockValue, personaBlockValue);
        humanBlockValues.put(agentName,humanBlockValue);
        personaBlockValues.put(agentName,personaBlockValue);
    }

    @Override
    public String getHumanBlockValue(String agentName) {
        return humanBlockValues.get(agentName);
    }

    @Override
    public String getPersonaBlockValue(String agentName) {
        return personaBlockValues.get(agentName);
    }
}
