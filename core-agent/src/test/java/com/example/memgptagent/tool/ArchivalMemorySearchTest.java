package com.example.memgptagent.tool;

import com.example.memgptagent.BaseTest;
import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentManager;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("postgresql")
@TestPropertySource(properties = {"spring.ai.vectorstore.pgvector.initialize-schema=true",
        "spring.ai.vectorstore.pgvector.dimensions=384"})
public class ArchivalMemorySearchTest extends BaseTest {

    @Autowired
    PostgreSQLContainer<?> container;

    @Autowired
    EmbeddingModel embeddingModel;

    @Autowired
    AgentManager agentManager;

    @Test
    public void testArchivalMemorySearch() throws Exception{

        var embFile = new ClassPathResource("embeddings/thingsILearned.dat");
        var queryFile = new ClassPathResource("embeddings/whatHaveILearned.dat");


        BufferedReader reader = new BufferedReader(new InputStreamReader(embFile.getInputStream()));
        String line;
        List<Float> values = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            values.add(Float.parseFloat(line));
        }

        float[] embeddings = ArrayUtils.toPrimitive(values.toArray(new Float[0]));
        when(embeddingModel.embed(any(), any(), any())).thenReturn(List.of(embeddings));


        reader = new BufferedReader(new InputStreamReader(queryFile.getInputStream()));
        values = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            values.add(Float.parseFloat(line));
        }

        float[] queryEmb = ArrayUtils.toPrimitive(values.toArray(new Float[0]));
        when(embeddingModel.embed(anyString())).thenReturn(queryEmb);

        Agent agent = getAgent(createNewAgent());

        agentManager.insertPassage(agent.getId(), "Some things the I learned: don't touch a hot skillet");

        ArchivalMemorySearch searchTool = new ArchivalMemorySearch(agent, agentManager);

        var request = new ArchivalMemorySearch.ArchivalMemorySearchRequest("", "What have I learned", 0, 0, true);

        ToolResponse resp = searchTool.apply(request);

        assertTrue(resp.message().contains("Some things the I learned: don't touch a hot skillet"));

    }
}
