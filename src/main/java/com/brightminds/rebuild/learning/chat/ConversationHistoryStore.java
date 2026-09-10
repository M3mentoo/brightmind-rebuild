package com.brightminds.rebuild.learning.chat;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class ConversationHistoryStore {

    private final ConcurrentMap<String, ArrayDeque<ConversationMessage>> histories = new ConcurrentHashMap<>();
    private final int maxMessages;

    public ConversationHistoryStore(TutorProperties properties) {
        this.maxMessages = properties.getHistoryMaxMessages();
    }

    public List<ConversationMessage> snapshot(String sessionId) {
        ArrayDeque<ConversationMessage> history = histories.get(sessionId);
        if (history == null) {
            return List.of();
        }
        synchronized (history) {
            return List.copyOf(history);
        }
    }

    public void appendTurn(String sessionId, String userMessage, String assistantMessage) {
        ArrayDeque<ConversationMessage> history = histories.computeIfAbsent(
                sessionId, ignored -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(new ConversationMessage(ConversationRole.USER, userMessage, Instant.now()));
            history.addLast(new ConversationMessage(ConversationRole.ASSISTANT, assistantMessage, Instant.now()));
            while (history.size() > maxMessages) {
                history.removeFirst();
                history.removeFirst();
            }
        }
    }
}
