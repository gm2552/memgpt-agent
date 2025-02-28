package com.example.memgptagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Table(name = "agents")
@Entity
public class Agent extends VersionedEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, name = "agent_name")
    private String agentName;

    @CreationTimestamp
    @Column(updatable = false, name = "create_dt_time")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "update_dt_time")
    private LocalDateTime updatedAt;

    @Column(nullable = false, name = "message_ids")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<UUID> messageIds;

    @Column(nullable = false, name = "tool_ids")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<UUID> toolIds;

    @Column(nullable = false, name = "system_prompt", length=50000)
    private String systemPrompt;

    @Column(nullable = false, name = "context_window")
    private int contextWindow;

    @Column(nullable = false, name = "metadata_labels")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadataLabels;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<UUID> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<UUID> messageIds) {
        this.messageIds = messageIds;
    }

    public List<UUID> getToolIds() {
        return toolIds;
    }

    public void setToolIds(List<UUID> toolIds) {
        this.toolIds = toolIds;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public int getContextWindow() {
        return contextWindow;
    }

    public void setContextWindow(int contextWindow) {
        this.contextWindow = contextWindow;
    }

    public Map<String, Object> getMetadataLabels() {
        return metadataLabels;
    }

    public void setMetadataLabels(Map<String, Object> metadataLabels) {
        this.metadataLabels = metadataLabels;
    }
}
