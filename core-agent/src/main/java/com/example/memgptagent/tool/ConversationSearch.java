package com.example.memgptagent.tool;

import com.example.memgptagent.service.Agent;
import com.example.memgptagent.service.AgentManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

public class ConversationSearch implements Function<ConversationSearch.ConversationSearchRequest, ToolResponse>  {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationSearch.class);

    private static final int RETRIEVAL_QUERY_DEFAULT_PAGE_SIZE = 5;

    private final Agent agent;

    private final AgentManager agentManager;

    public ConversationSearch(Agent agent, AgentManager agentManager) {
        this.agentManager = agentManager;
        this.agent = agent;
    }

    @Override
    public ToolResponse apply(ConversationSearchRequest conversationSearchRequest) {

        LOGGER.debug("Conversation search request initiated for agent: {}", agent.getName());

        // get a count first
        int count = (int)agentManager.getMatchingUserMessagesCount(agent.getId(), conversationSearchRequest.query);

        int page = conversationSearchRequest.page == null ? 0 : conversationSearchRequest.page.intValue();

        int pageSize = RETRIEVAL_QUERY_DEFAULT_PAGE_SIZE;

        List<String> msgs = agentManager.getMatchingUserMessages(agent.getId(), conversationSearchRequest.query, page, pageSize)
                .stream().map(msg -> msg.content()).toList();

        if (msgs.isEmpty()){
            LOGGER.debug("Conversation search for agent {} found no messages", agent.getName());
            ToolResponse.contentOKStatus("No results found.");
        }

        int numPages = (count % pageSize > 0) ? (count / pageSize) + 1 : count / pageSize;

        StringBuilder builder = new StringBuilder("Showing " ).append(msgs.size()).append(" of ").append(count).append(" results ");
        builder.append(" (page ").append(page).append(" of ").append(numPages).append("\n");
        builder.append(msgs.toString());

        LOGGER.debug("Conversation search for agent {} found {} messages in page {} of {}", agent.getName(), msgs.size(),
                page, numPages);

        return ToolResponse.contentOKStatus(builder.toString());
    }

    public record ConversationSearchRequest(@JsonProperty(required = true)
                                            @JsonPropertyDescription("Deep inner monologue private to you only.") String innerThoughts,
                                            @JsonProperty(required = true)
                                            @JsonPropertyDescription("String to search for") String query,
                                            @JsonPropertyDescription("Allows you to page through results. Only use on a follow-up query. Defaults to 0 (first page).") Integer page,
                                            @JsonProperty(required = true)
                                            @JsonPropertyDescription("Request an immediate heartbeat after function execution. Set to `True` if you want to send a follow-up message or run a follow-up function.") boolean requestHeartbeat) {}

}
