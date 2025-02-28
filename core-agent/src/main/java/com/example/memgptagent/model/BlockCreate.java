package com.example.memgptagent.model;

import java.util.Map;

public record BlockCreate(String label, String description, int limit, Map<String, Object> metadata) {};
