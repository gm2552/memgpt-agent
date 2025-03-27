package com.example.memgptagent.service;

public interface AgentMetrics {
    Integer getContextSize(String agentName);

    void setContextSize(String agentName, Integer integer);

    String getHumanBlockValue(String agentName);

    String getPersonaBlockValue(String agentName);

    void setCoreMemory(String agentName, String humanBlockValue, String personaBlockValue);
}
