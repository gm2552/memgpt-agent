package com.example.memgptagent.repository;

import com.example.memgptagent.entity.Tool;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface ToolRepository extends CrudRepository<Tool, UUID> {

    List<Tool> findByCore(boolean tag);

}
