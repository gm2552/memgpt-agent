package com.example.memgptagent.repository;

import com.example.memgptagent.entity.Block;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface BlockRepository extends CrudRepository<Block, UUID> {

    List<Block> findBlockByAgentId(UUID agentId);
}
