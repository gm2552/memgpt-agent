package com.example.memgptagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "tools")
@Entity
public class Tool {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(nullable = false, name = "fq_class_name")
    private String fqClassName;

    @Column(name = "fq_input_class_name")
    private String fqInputClassName;

    @Column(name = "return_character_limit")
    private Integer returnCharacterLimit;

    @Column(name = "core")
    private boolean core;

    @CreationTimestamp
    @Column(updatable = false, name = "create_dt_time")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "update_dt_time")
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFqClassName() {
        return fqClassName;
    }

    public void setFqClassName(String fqClassName) {
        this.fqClassName = fqClassName;
    }

    public String getFqInputClassName() {
        return fqInputClassName;
    }

    public void setFqInputClassName(String fqInputClassName) {
        this.fqInputClassName = fqInputClassName;
    }

    public Integer getReturnCharacterLimit() {
        return returnCharacterLimit;
    }

    public void setReturnCharacterLimit(Integer returnCharacterLimit) {
        this.returnCharacterLimit = returnCharacterLimit;
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

    public boolean isCore() {
        return core;
    }

    public void setCore(boolean core) {
        this.core = core;
    }
}
