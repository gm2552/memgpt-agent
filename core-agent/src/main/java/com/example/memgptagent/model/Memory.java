package com.example.memgptagent.model;

import java.util.Optional;
import java.util.Map;

public record Memory(Map<String, Block> blocks) {

    Optional<Block> getBlockByLabel(String label) {
        Block block = blocks.get(label);
        return block == null ? Optional.empty() : Optional.of(block);
    }

    private static final String MEMORY_BLOCK_TEMPLATE = """
            <%s characters="%d/%d">
            %s
            <%s>
            """;

    public String getCompiledMemoryBlock() {

        final StringBuilder builder = new StringBuilder();

        blocks.values().forEach(block -> {

            String compiledBlock = String.format(MEMORY_BLOCK_TEMPLATE, block.label(), block.value().length(),
                    block.limit(), block.value(), block.label());

            builder.append(compiledBlock).append("\n");
        });

        return builder.toString();
    }

}
