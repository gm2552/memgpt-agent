package com.example.memgptagent.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AgentState(UUID id, String name, String description, String systemPrompt,
                         List<UUID> messageIds, List<Tool> tools, int contextWindowSize,
                         Memory memory, Map<String, Object> metadata) {
}
