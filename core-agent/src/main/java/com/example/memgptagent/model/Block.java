package com.example.memgptagent.model;

import java.util.Map;
import java.util.UUID;

public record Block(UUID id, String value, int limit, String label, String description,
                    Map<String, Object> metadata) {
}
