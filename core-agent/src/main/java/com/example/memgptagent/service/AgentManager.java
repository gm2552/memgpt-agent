package com.example.memgptagent.service;

import com.example.memgptagent.model.AgentCreate;
import com.example.memgptagent.model.AgentState;
import com.example.memgptagent.model.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentManager {

    Optional<AgentState> getAgentStateById(UUID id);

    Optional<AgentState> getAgentStateByName(String name);

    void clearAgentStateByName(String name);

    AgentState createAgent(AgentCreate agentCreate);

    List<Message> getMessagesByIds(List<UUID> ids);

    List<Message> getMessagesByAgentId(UUID agentId);

    List<Message> getMatchingUserMessages(UUID agentId, String queryText, int pageNum, int pageSize);

    long getMatchingUserMessagesCount(UUID agentId, String queryText);

    void saveNewMessages(UUID agentId, List<Message> messages);

    void replaceContextWithNewMessages(UUID agentId, List<Message> messages);

    void updateMemoryBlockValue(UUID blockId, String value);

    void insertPassage(UUID agentId, String passage);

    List<String> getMatchingUserPassages(UUID agentId, String queryText);

}
