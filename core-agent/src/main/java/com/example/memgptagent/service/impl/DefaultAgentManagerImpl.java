package com.example.memgptagent.service.impl;

import com.example.memgptagent.entity.Agent;
import com.example.memgptagent.model.AgentCreate;
import com.example.memgptagent.model.AgentState;
import com.example.memgptagent.model.Block;
import com.example.memgptagent.model.BlockCreate;
import com.example.memgptagent.model.Memory;
import com.example.memgptagent.model.Message;
import com.example.memgptagent.model.Tool;
import com.example.memgptagent.repository.AgentRepository;
import com.example.memgptagent.repository.BlockRepository;
import com.example.memgptagent.repository.MessageRepository;
import com.example.memgptagent.service.AgentManager;
import com.example.memgptagent.service.ToolManager;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class DefaultAgentManagerImpl implements AgentManager {

    private static final float DEFAULT_MEMORY_THRESHOLD = .75f;

    private final AgentRepository agentRepository;

    private final BlockRepository blockRepository;

    private final ToolManager toolManager;

    private final MessageRepository messageRepository;

    private final VectorStore vectorStore;

    public DefaultAgentManagerImpl(AgentRepository agentRepository, BlockRepository blockRepository,
                                   ToolManager toolManager, MessageRepository messageRepository,
                                   ObjectProvider<VectorStore> vectorStore) {
        this.agentRepository = agentRepository;
        this.blockRepository = blockRepository;
        this.toolManager = toolManager;
        this.messageRepository = messageRepository;
        this.vectorStore = vectorStore.getIfAvailable();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Optional<AgentState> getAgentStateById(UUID id) {
        return agentRepository.findById(id).map(agent -> Optional.of(retrieveAgentState(agent)))
                .orElse(Optional.empty());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Optional<AgentState> getAgentStateByName(String name) {
        return agentRepository.findByAgentName(name).map(agent -> Optional.of(retrieveAgentState(agent)))
                .orElse(Optional.empty());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void clearAgentStateByName(String name) {

        getAgentStateByName(name).ifPresent(agent -> {

            // clear all messages
            messageRepository.deleteByAgentId(agent.id());

            // clear memory state
            agent.memory().blocks().values().forEach(block -> {
                this.updateMemoryBlockValue(block.id(), "");
            });

            // TODO: Clear archival memory


            // clear out the context message list
            replaceContextWithNewMessages(agent.id(), List.of());

        });

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public AgentState createAgent(AgentCreate agentCreate) {

        // get core tools
        List<UUID> toolIds = agentCreate.toolIds().isEmpty() ? Streamable.of(toolManager.getCoreTools()).stream().map(tool -> tool.id()).collect(Collectors.toUnmodifiableList()) :
                agentCreate.toolIds();

        // create the human and personal Core memory blocks
        BlockCreate humanBlock = new BlockCreate("human", "Person",5000, Map.of());
        BlockCreate personaBlock = new BlockCreate("persona", "Personal",5000, Map.of());

        List<BlockCreate> defaultBlocks = List.of(humanBlock, personaBlock);

        List<BlockCreate> blocks = agentCreate.blocks().isEmpty() ? defaultBlocks : agentCreate.blocks();

        Agent newAgent = new Agent();
        newAgent.setAgentName(agentCreate.name());
        newAgent.setSystemPrompt(StringUtils.hasText(agentCreate.systemPrompt()) ? agentCreate.systemPrompt() : DefaultAgentContet.COMPLETION_SYSTEM_PROMPT);
        newAgent.setDescription(agentCreate.description());
        newAgent.setMetadataLabels(agentCreate.metadata() != null ? agentCreate.metadata() : Map.of());
        newAgent.setContextWindow(agentCreate.contextWindowSize());
        newAgent.setMessageIds(List.of());
        newAgent.setToolIds(toolIds);
        newAgent.setSummaryThreshold(agentCreate.summaryThreshold() > 0.0f ? agentCreate.summaryThreshold() : DEFAULT_MEMORY_THRESHOLD);

        UUID savedAgentId = agentRepository.save(newAgent).getId();

        // create blocks
        List<com.example.memgptagent.entity.Block> addBlocks = blocks.stream().map(block -> {

            var newBlock = new com.example.memgptagent.entity.Block();
            newBlock.setValue("");
            newBlock.setAgentId(savedAgentId);
            newBlock.setMetadata(block.metadata());
            newBlock.setLabel(block.label());
            newBlock.setDescription(block.description());
            newBlock.setLimit(block.limit());

            return newBlock;

        }).collect(Collectors.toUnmodifiableList());

        if (!addBlocks.isEmpty())
            blockRepository.saveAll(addBlocks);

        return this.getAgentStateById(savedAgentId).get();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Message> getMessagesByIds(List<UUID> ids) {

        var messages = Streamable.of(messageRepository.findAllById(ids))
                .stream().sorted((msg1, msg2) -> msg1.getCreatedAt().isAfter(msg2.getCreatedAt()) ? 1 : -1)
                .collect(Collectors.toList());

        return messages.stream().map(msg ->
                new Message(msg.getId(), MessageType.fromValue(msg.getRole()), msg.getText(), msg.getToolCalls(),
                        msg.getToolCallId(), msg.getStepId()))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Message> getMessagesByAgentId(UUID agentId) {

        return messageRepository.findByAgentId(agentId).stream().map(msg ->
                new Message(msg.getId(), MessageType.fromValue(msg.getRole()), msg.getText(), msg.getToolCalls(),
                        msg.getToolCallId(), msg.getStepId()))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Message> getMatchingUserMessages(UUID agentId, String queryText, int pageNum, int pageSize) {

        PageRequest pageAndSort =
                PageRequest.of(pageNum, pageSize, Sort.by("createdAt").ascending());

        return messageRepository.findAllByAgentIdAndRoleAndTextContainingIgnoreCase(agentId,
                MessageType.USER.getValue(), queryText, pageAndSort).stream()
                .map(msg -> new Message(msg.getId(), MessageType.fromValue(msg.getRole()), msg.getText(), msg.getToolCalls(),
                        msg.getToolCallId(), msg.getStepId())).toList();

    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public long getMatchingUserMessagesCount(UUID agentId, String queryText) {
        return messageRepository.countByAgentIdAndRoleAndTextContainingIgnoreCase(agentId, MessageType.USER.getValue(), queryText);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveNewMessages(UUID agentId, List<Message> messages) {

        List<com.example.memgptagent.entity.Message> newSaveMessages = messages.stream().map(msg -> {

            com.example.memgptagent.entity.Message saveMsg = new com.example.memgptagent.entity.Message();
            saveMsg.setAgentId(agentId);
            saveMsg.setName(UUID.randomUUID().toString());
            saveMsg.setModel("");
            saveMsg.setRole(msg.role().getValue());
            saveMsg.setStepId(msg.stepId());
            saveMsg.setToolCallId(msg.toolCallId());
            saveMsg.setText(msg.content());
            saveMsg.setToolCalls(msg.toolCalls());

            return saveMsg;


        }).collect(Collectors.toUnmodifiableList());

        List<UUID> newIDs = StreamSupport
                .stream(messageRepository.saveAll(newSaveMessages).spliterator(), false)
                .map(msg -> msg.getId())
                .toList();

        // get the agent and update the message ids
        agentRepository.findById(agentId).ifPresent(agent -> {

            List<UUID> msgIds = new ArrayList<>(agent.getMessageIds());
            msgIds.addAll(newIDs);

            agent.setMessageIds(msgIds);

            agentRepository.save(agent);
        });

    }

    @Override
    public void replaceContextWithNewMessages(UUID agentId, List<Message> messages) {

        List<com.example.memgptagent.entity.Message> newSaveMessages = messages.stream().map(msg -> {

            com.example.memgptagent.entity.Message saveMsg = new com.example.memgptagent.entity.Message();
            saveMsg.setAgentId(agentId);
            saveMsg.setName(UUID.randomUUID().toString());
            saveMsg.setModel("");
            saveMsg.setRole(msg.role().getValue());
            saveMsg.setStepId(msg.stepId());
            saveMsg.setToolCallId(msg.toolCallId());
            saveMsg.setText(msg.content());
            saveMsg.setToolCalls(msg.toolCalls());

            return saveMsg;


        }).collect(Collectors.toUnmodifiableList());

        List<UUID> newIDs = newSaveMessages.isEmpty() ? List.of() :
                 StreamSupport.stream(messageRepository.saveAll(newSaveMessages).spliterator(), false)
                .map(msg -> msg.getId())
                .toList();

        // get the agent and update the message ids
        agentRepository.findById(agentId).ifPresent(agent -> {

            agent.setMessageIds(newIDs);

            agentRepository.save(agent);
        });

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateMemoryBlockValue(UUID blockId, String value) {

        blockRepository.findById(blockId).ifPresent(block -> {
            block.setValue(value);
            blockRepository.save(block);
        });
    }

    @Override
    public void insertPassage(UUID agentId, String passage) {

        if (vectorStore != null) {

            // annotate with the agent id
            List<Document> docs = new TokenTextSplitter().split(new Document(passage, Map.of("agentId", agentId.toString())));

            // persist to the vector store
            vectorStore.add(docs);
        }

    }

    @Override
    public List<String> getMatchingUserPassages(UUID agentId, String queryText) {

        // make sure the returned documents are only for the requested agent id
        Filter.Expression metaExpression = new FilterExpressionBuilder().eq("agentId", agentId.toString()).build();

        SearchRequest searchRequest = SearchRequest.builder().query(queryText).topK(5)
               .filterExpression(metaExpression).build();

        // simple vector store query
        return vectorStore.similaritySearch(searchRequest).stream().map(doc -> doc.getText()).toList();

    }

    @Override
    public Optional<UUID> getAgentIdByName(String agentName) {
        return agentRepository.findByAgentName(agentName).map(agent -> Optional.of(agent.getId()))
                .orElse(Optional.empty());
    }

    @Override
    public Optional<Integer> getAgentContextWindowMessageCount(UUID agentId) {
        return agentRepository.findById(agentId).map(agent -> Optional.of(agent.getMessageIds().size()))
                .orElse(Optional.empty());
    }

    @Override
    public Optional<Long> getAgentTotalMessageCount(UUID agentId) {
        return Optional.empty();
    }

    private AgentState retrieveAgentState(Agent agent) {
        Mono<Tuple3<Agent, Memory, List<Tool>>> zippedMono =
                Mono.zip(Mono.just(agent), getAgentStateMemory(agent.getId()), getAgentStateTools(agent.getToolIds()));

        return zippedMono.map(tuple -> {
            return new AgentState(tuple.getT1().getId(), tuple.getT1().getAgentName(), tuple.getT1().getDescription(),
                    tuple.getT1().getSystemPrompt(), tuple.getT1().getMessageIds(), tuple.getT3(), tuple.getT1().getContextWindow(),
                    tuple.getT1().getSummaryThreshold(),
                    tuple.getT2(), tuple.getT1().getMetadataLabels());
        }).block();
    }

    private Mono<Memory> getAgentStateMemory(UUID agentId) {

        return Flux.fromIterable(blockRepository.findBlockByAgentId(agentId))
                .map(block -> new Block(block.getId(), block.getValue(), block.getLimit(), block.getLabel(),
                        block.getDescription(), block.getMetadata()))
                .collectList()
                .map(blocks ->  {
                    Map<String, Block> blockMap = blocks.stream().collect(
                            Collectors.toMap(block -> block.label(), Function.identity()));

                    return new Memory(blockMap);
                });
    }

    private Mono<List<Tool>> getAgentStateTools(List<UUID> toolIds) {

        return Flux.fromIterable(toolManager.getToolsByIds(toolIds)).collectList();
    }
}
