package com.brightminds.rebuild.learning.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.brightminds.rebuild.learning.session.LearningSessionResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

class TutorChatServiceTest {

    @Test
    void shouldOrchestrateEventsAndSaveCompletedTurn() {
        TutorProperties properties = properties(Duration.ofSeconds(1));
        ConversationHistoryStore historyStore = new ConversationHistoryStore(properties);
        TutorModelClient client = prompt -> Flux.just("你", "好");
        TutorChatService service = new TutorChatService(
                client, new TutorPromptFactory(), historyStore, properties);

        List<ServerSentEvent<ChatStreamPayload>> events = service
                .streamReply(session(), " 你好？ ")
                .collectList()
                .block(Duration.ofSeconds(1));

        assertThat(events).extracting(ServerSentEvent::event)
                .containsExactly("start", "delta", "delta", "done");
        assertThat(events.getLast().data().content()).isEqualTo("你好");
        assertThat(historyStore.snapshot("session-1"))
                .extracting(ConversationMessage::content)
                .containsExactly("你好？", "你好");
    }

    @Test
    void shouldEmitErrorEventAndNotSavePartialHistoryWhenModelTimesOut() {
        TutorProperties properties = properties(Duration.ofMillis(20));
        ConversationHistoryStore historyStore = new ConversationHistoryStore(properties);
        TutorModelClient client = prompt -> Flux.never();
        TutorChatService service = new TutorChatService(
                client, new TutorPromptFactory(), historyStore, properties);

        List<ServerSentEvent<ChatStreamPayload>> events = service
                .streamReply(session(), "你好？")
                .collectList()
                .block(Duration.ofSeconds(1));

        assertThat(events).extracting(ServerSentEvent::event).containsExactly("start", "error");
        assertThat(events.getLast().data().errorCode()).isEqualTo("MODEL_TIMEOUT");
        assertThat(historyStore.snapshot("session-1")).isEmpty();
    }

    private TutorProperties properties(Duration timeout) {
        TutorProperties properties = new TutorProperties();
        properties.setStreamIdleTimeout(timeout);
        properties.setHistoryMaxMessages(20);
        return properties;
    }

    private LearningSessionResponse session() {
        return new LearningSessionResponse(
                "session-1", "小明", 6, "恐龙", "ACTIVE", Instant.parse("2026-09-10T08:00:00Z"));
    }
}
