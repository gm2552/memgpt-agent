package com.example.memgptagent.tool;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import com.example.memgptagent.BaseTest;
import com.example.memgptagent.service.Agent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("postgresql")
public class ArchivalMemoryInsertTest extends BaseTest {

    @Autowired
    PostgreSQLContainer<?> container;

    @Autowired
    EmbeddingModel embeddingModel;

    @BeforeEach
    public void init() throws IOException, InterruptedException {

        // enable PG vector extension
        // probably better way to do this
        Container.ExecResult res = container.execInContainer("psql", "-U",  "test",  "-d",  "test", "-c",  "CREATE EXTENSION vector");
    }

    //@Test
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

        List<Document> docs = new TokenTextSplitter().split(new Document("Some things the I learned: don't touch a hot skillet", Map.of("agentId", agent.getId())));

        tool.apply(request);

    }

}
