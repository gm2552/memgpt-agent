package com.example.memgptagent.service.impl;

import com.example.memgptagent.model.Tool;
import com.example.memgptagent.repository.ToolRepository;
import com.example.memgptagent.service.ToolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DefaultToolManager implements ToolManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultToolManager.class);

    private final ToolRepository toolRepository;

    public DefaultToolManager(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<Tool> addTools(List<Tool> tools) {
        List<com.example.memgptagent.entity.Tool> addedTools = tools.stream().map(DefaultToolManager::toEnityTool).toList();

        return Streamable.of(toolRepository.saveAll(addedTools)).stream()
                .map(DefaultToolManager::toModelTool).toList();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Tool> getAllTools() {
        return Streamable.of(toolRepository.findAll()).stream()
                .map(DefaultToolManager::toModelTool).collect(Collectors.toUnmodifiableList());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Tool> getCoreTools() {
        return Streamable.of(toolRepository.findByCore(true)).stream()
                .map(DefaultToolManager::toModelTool).collect(Collectors.toUnmodifiableList());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Tool> getToolsByIds(List<UUID> ids) {
        return Streamable.of(toolRepository.findAllById(ids)).stream()
                .map(DefaultToolManager::toModelTool).collect(Collectors.toUnmodifiableList());
    }

    private static Tool toModelTool(com.example.memgptagent.entity.Tool tool) {

        Class<?> toolClass = null;
        Class<?> inputTypeClass = null;

        try {
            toolClass = Class.forName(tool.getFqClassName());
            inputTypeClass = StringUtils.hasText(tool.getFqInputClassName()) ?
                    Class.forName(tool.getFqInputClassName()) : null;
        }
        catch (Exception e) {
            LOGGER.warn("Error retrieving tool {}", tool.getName(), e);
        }


        return new Tool(tool.getId(), tool.getName(), tool.getDescription(), toolClass,
                inputTypeClass, tool.getReturnCharacterLimit(), tool.isCore());

    }

    private static com.example.memgptagent.entity.Tool toEnityTool(Tool tool) {

        com.example.memgptagent.entity.Tool newTool = new com.example.memgptagent.entity.Tool();
        newTool.setName(tool.name());
        newTool.setFqClassName(tool.toolClass().getName());
        if (tool.inputTypeClass() != null)
            newTool.setFqInputClassName(tool.inputTypeClass().getName());
        newTool.setDescription(tool.description());
        newTool.setCore(tool.core());

        return newTool;

    }

}
