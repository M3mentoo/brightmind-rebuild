package com.brightminds.rebuild.learning.chat;

import java.time.Instant;

public record ConversationMessage(
        ConversationRole role,
        String content,
        Instant createdAt) {
}
