package com.example.memgptagent.repository;

import com.example.memgptagent.entity.Tool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.Assert.assertNotNull;

@SpringBootTest
public class ToolRepositoryTest {

    @Autowired
    private ToolRepository toolRepository;

    @Test
    public void testSaveGetTool() {

        Tool tool = new Tool();
        tool.setDescription("Test tool");
        tool.setName("Test tool");
        tool.setReturnCharacterLimit(1000);
        tool.setFqClassName("java.lang.Map");
        tool.setFqInputClassName("java.util.List");

        UUID toolId = toolRepository.save(tool).getId();

        Tool savedTool = toolRepository.findById(toolId).get();

        assertNotNull(savedTool);

    }

}
