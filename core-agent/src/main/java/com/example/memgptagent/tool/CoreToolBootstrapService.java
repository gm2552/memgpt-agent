package com.example.memgptagent.tool;


import com.example.memgptagent.entity.Tool;
import com.example.memgptagent.repository.ToolRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoreToolBootstrapService implements ApplicationListener<ContextRefreshedEvent>
{

    private final ToolRepository toolRepository;

    public CoreToolBootstrapService(ToolRepository toolRepository) {

        this.toolRepository = toolRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        // TODO: Hard coded list for now.  Create a scan algorithm to find tools

        Tool memAppendTool = new Tool();
        memAppendTool.setName("core_memory_append");
        memAppendTool.setFqClassName(CoreMemoryAppend.class.getName());
        memAppendTool.setFqInputClassName(CoreMemoryAppend.MemoryAppendRequest.class.getName());
        memAppendTool.setDescription("Append to the contents of core memory.");

        Tool sendMessageTool = new Tool();
        sendMessageTool.setName("send_message");
        sendMessageTool.setFqClassName(SendMessage.class.getName());
        sendMessageTool.setFqInputClassName(SendMessage.SendMessageRequest.class.getName());
        sendMessageTool.setDescription("Sends a message to the human user.");

        Tool archiveMemoryInsertTool = new Tool();
        archiveMemoryInsertTool.setName("archival_memory_insert");
        archiveMemoryInsertTool.setFqClassName(ArchivalMemoryInsert.class.getName());
        archiveMemoryInsertTool.setFqInputClassName(ArchivalMemoryInsert.ArchivalMemoryInsertRequest.class.getName());
        archiveMemoryInsertTool.setDescription("Add to archival memory. Make sure to phrase the memory contents such that it can be easily queried later.");

        Tool memoryReplaceTool = new Tool();
        memoryReplaceTool.setName("core_memory_replace");
        memoryReplaceTool.setFqClassName(CoreMemoryReplace.class.getName());
        memoryReplaceTool.setFqInputClassName(CoreMemoryReplace.MemoryReplaceRequest.class.getName());
        memoryReplaceTool.setDescription("Replace the contents of core memory. To delete memories, use an empty string for new_content.");

        Tool archiveMemSearchTool = new Tool();
        archiveMemSearchTool.setName("archival_memory_search");
        archiveMemSearchTool.setFqClassName(ArchivalMemorySearch.class.getName());
        archiveMemSearchTool.setFqInputClassName(ArchivalMemorySearch.ArchivalMemorySearchRequest.class.getName());
        archiveMemSearchTool.setDescription("Search archival memory using semantic (embedding-based) search.");

        Tool convserationSearchTool = new Tool();
        convserationSearchTool.setName("conversation_search");
        convserationSearchTool.setFqClassName(ConversationSearch.class.getName());
        convserationSearchTool.setFqInputClassName(ConversationSearch.ConversationSearchRequest.class.getName());
        convserationSearchTool.setDescription("Search prior conversation history using case-insensitive string matching.");

        toolRepository.saveAll(List.of(memAppendTool, sendMessageTool, archiveMemoryInsertTool, memoryReplaceTool,
                archiveMemSearchTool, convserationSearchTool));

    }

}
