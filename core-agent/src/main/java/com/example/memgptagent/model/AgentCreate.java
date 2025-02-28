package com.example.memgptagent.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AgentCreate(String name, String description, String systemPrompt, List<UUID> toolIds,
                          int contextWindowSize, List<BlockCreate> blocks, Map<String, Object> metadata) {

}
