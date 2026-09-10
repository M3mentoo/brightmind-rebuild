package com.brightminds.rebuild.learning.chat;

public record ChatStreamPayload(
        String sessionId,
        String messageId,
        String content) {
}
