package com.example.memgptagent.repository;

import com.example.memgptagent.entity.Agent;
import com.example.memgptagent.entity.Block;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;

@SpringBootTest
public class BlockRepositoryTest {

    @Autowired
    private BlockRepository blockRepository;

    @Test
    public void testSaveGetBlock() {

        Block block = new Block();
        block.setAgentId(UUID.randomUUID());
        block.setDescription("test description");
        block.setLabel("human");
        block.setMetadata(Map.of());
        block.setValue("Name: Jim");

        UUID blockId = blockRepository.save(block).getId();

        Block savedBlock = blockRepository.findById(blockId).get();

        assertNotNull(savedBlock);

    }
}
