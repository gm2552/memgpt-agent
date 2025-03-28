package com.example.memgptagent.tool;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.ai.embedding.EmbeddingModel;
import com.example.memgptagent.BaseTest;
import com.example.memgptagent.service.Agent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("postgresql")
@TestPropertySource(properties = {"spring.ai.vectorstore.pgvector.initialize-schema=true",
        "spring.ai.vectorstore.pgvector.dimensions=384"})
public class ArchivalMemoryInsertTest extends BaseTest {

    @Autowired
    PostgreSQLContainer<?> container;

    @Autowired
    EmbeddingModel embeddingModel;

    @Test
    public void testInsertIntoArchivalMemory() throws Exception {

        var embFile = new ClassPathResource("embeddings/thingsILearned.dat");

        BufferedReader reader = new BufferedReader(new InputStreamReader(embFile.getInputStream()));
        String line;
        List<Float> values = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            values.add(Float.parseFloat(line));
        }

        float[] embeddings = ArrayUtils.toPrimitive(values.toArray(new Float[0]));

        when(embeddingModel.embed(any(), any(), any())).thenReturn(List.of(embeddings));

        Agent agent = getAgent(createNewAgent());

        var tool = new ArchivalMemoryInsert(agent, this.agentManager);

        var request = new ArchivalMemoryInsert.ArchivalMemoryInsertRequest("",
                                                  "Some things the I learned: don't touch a hot skillet", true);
        tool.apply(request);

    }

}
