package com.example.memgptagent.service;

import com.example.memgptagent.model.Tool;

import java.util.List;
import java.util.UUID;

public interface ToolManager {

    List<Tool> addTools(List<Tool> tools);

    List<Tool> getAllTools();

    List<Tool> getCoreTools();

    List<Tool> getToolsByIds(List<UUID> ids);

}
