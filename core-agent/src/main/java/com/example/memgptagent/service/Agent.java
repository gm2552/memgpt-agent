package com.example.memgptagent.service;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public interface Agent {

    UUID getId();

    String getName();

    AssistantMessage chat(OpenAiApi.ChatCompletionRequest chatRequest);

    List<Message> recallContext(UserMessage request);

    void appendContext(List<Message> messages);

    Flux<OpenAiApi.ChatCompletionChunk> streamChat(OpenAiApi.ChatCompletionRequest chatRequest);
}
