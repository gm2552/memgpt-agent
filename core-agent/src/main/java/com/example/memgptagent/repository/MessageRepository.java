package com.example.memgptagent.repository;

import com.example.memgptagent.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends CrudRepository<Message, UUID>,
        PagingAndSortingRepository<Message, UUID>  {

    List<Message> findByAgentId(UUID agentId);

    List<Message> findAllByAgentIdAndRoleAndTextContainingIgnoreCase(UUID agentId, String role, String text, Pageable page);

    long  countByAgentIdAndRoleAndTextContainingIgnoreCase(UUID agentId, String role, String text);

    void deleteByAgentId(UUID agentId);
}
