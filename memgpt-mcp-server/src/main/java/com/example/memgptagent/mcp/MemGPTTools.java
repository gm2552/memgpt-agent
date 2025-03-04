package com.example.memgptagent.mcp;

import com.example.memgptagent.model.AgentCreate;
import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentLoader;
import com.example.memgptagent.service.AgentManager;
import io.micrometer.common.util.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MemGPTTools {

    private final AgentLoader agentLoader;

    private final AgentManager agentManager;

    private final ChatClient.Builder chatClientBuilder;

    private final int contextWindowSize;

    public MemGPTTools(AgentLoader agentLoader, AgentManager agentManager, ChatClient.Builder chatClientBuilder,
                       @Value("${memgpt.agent.contextWindowsSize:16384}") int contextWindowSize) {

        this.agentLoader = agentLoader;
        this.agentManager = agentManager;
        this.chatClientBuilder = chatClientBuilder;
        this.contextWindowSize = contextWindowSize;
    }

    @Tool(name="chat", description="Executes a continued chat given a user provided prompt and the system " +
            " returns a response to the continues chat.  The chat context is specified by the agent name")
    public String chat(@ToolParam(description="The unique name of the agent that maintains chat context and history") String agentName,
                       @ToolParam(description="The users prompt message that continues the conversation.")String promptMessage) {

        if (StringUtils.isEmpty(agentName))
            return "Invalid agentId.  AgentId cannot be empty.";

        if (StringUtils.isEmpty(promptMessage))
            return "Invalid promptMessage. PromptMessage cannot be empty.";

        // load the agent and see check if the agent exists
        Optional<Agent> agent = agentLoader.loadAgentByName(agentName, chatClientBuilder.build());

        if (agent.isEmpty()) {

            AgentCreate agentCreate = new AgentCreate(agentName, "", "",
                    List.of(), contextWindowSize, List.of(), Map.of());

            // need to create a new agent/session
            agentManager.createAgent(agentCreate);

            // now load the agent again
            agent = agentLoader.loadAgentByName(agentName, chatClientBuilder.build());
        }

        OpenAiApi.ChatCompletionMessage msg =
                new OpenAiApi.ChatCompletionMessage(promptMessage, OpenAiApi.ChatCompletionMessage.Role.USER);

        OpenAiApi.ChatCompletionRequest req = new OpenAiApi.ChatCompletionRequest(List.of(msg), "", .5);

        String result = agent.get().chat(req).choices().getFirst().message().content();

        return result;

    }

}
