package com.example.memgptagent.service;

import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface Agent {

    UUID getId();

    String getName();

    OpenAiApi.ChatCompletion chat(OpenAiApi.ChatCompletionRequest chatRequest);

    Flux<OpenAiApi.ChatCompletionChunk> streamChat(OpenAiApi.ChatCompletionRequest chatRequest);
}
