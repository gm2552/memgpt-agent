package com.example.memgptagent.model;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;

import java.util.List;
import java.util.UUID;

public record Message(UUID id, MessageType role, String content, List<AssistantMessage.ToolCall> toolCalls,
                      String toolCallId, String stepId) {
}
