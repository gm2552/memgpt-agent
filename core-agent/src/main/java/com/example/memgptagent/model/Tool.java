package com.example.memgptagent.model;

import java.util.UUID;

public record Tool(UUID id, String name, String description, Class<?> toolClass,
                   Class<?> inputTypeClass, Integer returnCharaterLimit, boolean core) {
}
