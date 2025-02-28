package com.example.memgptagent.repository;

import com.example.memgptagent.entity.Agent;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentRepository extends CrudRepository<Agent, UUID> {

    Optional<Agent> findByAgentName(String agentName);

}
