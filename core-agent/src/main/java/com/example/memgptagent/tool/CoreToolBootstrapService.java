package com.example.memgptagent.tool;


import com.example.memgptagent.model.Tool;
import com.example.memgptagent.service.ToolManager;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CoreToolBootstrapService implements ApplicationListener<ContextRefreshedEvent>
{

    private final ToolManager toolManager;

    public CoreToolBootstrapService(ToolManager toolManager) {

        this.toolManager = toolManager;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        // TODO: Hard coded list of core tools for now.  Create a scan algorithm to find tools

        Tool memAppendTool = new Tool(null, "core_memory_append", "Append to the contents of core memory.",
                CoreMemoryAppend.class, CoreMemoryAppend.MemoryAppendRequest.class, 5000, true);

        Tool sendMessageTool = new Tool(null, "send_message", "Sends a message to the human user.",
                SendMessage.class, SendMessage.SendMessageRequest.class, 5000, true);

        Tool archiveMemoryInsertTool = new Tool(null, "archival_memory_insert", "Add to archival memory. Make sure to phrase the memory contents such that it can be easily queried later.",
                ArchivalMemoryInsert.class, ArchivalMemoryInsert.ArchivalMemoryInsertRequest.class, 5000, true);

        Tool memoryReplaceTool = new Tool(null, "core_memory_replace", "Replace the contents of core memory. To delete memories, use an empty string for new_content.",
                CoreMemoryReplace.class, CoreMemoryReplace.MemoryReplaceRequest.class, 5000, true);

        Tool archiveMemSearchTool = new Tool(null, "archival_memory_search", "Search archival memory using semantic (embedding-based) search.",
                ArchivalMemorySearch.class, ArchivalMemorySearch.ArchivalMemorySearchRequest.class, 5000, true);

        Tool convserationSearchTool = new Tool(null, "conversation_search", "Search prior conversation history using case-insensitive string matching.",
                ConversationSearch.class, ConversationSearch.ConversationSearchRequest.class, 5000, true);

        Tool retrievalDoneTool = new Tool(null, "retrieval_done", "Indicates that message editing and retrieval is complete.",
                RetrievalDone.class, null, 5000, true);

        toolManager.addTools(List.of(memAppendTool, sendMessageTool, archiveMemoryInsertTool, memoryReplaceTool,
                archiveMemSearchTool, convserationSearchTool, retrievalDoneTool));

    }

}
