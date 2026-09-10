package com.brightminds.rebuild.learning.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.brightminds.rebuild.learning.session.LearningSessionResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;

class SimulatedTutorServiceTest {

    private final SimulatedTutorService simulatedTutorService = new SimulatedTutorService(Duration.ZERO);

    @Test
    void shouldEmitStartDeltasAndDoneInOrder() {
        LearningSessionResponse session = new LearningSessionResponse(
                "session-1", "小明", 6, "恐龙", "ACTIVE", Instant.parse("2026-09-10T08:00:00Z"));

        List<ServerSentEvent<ChatStreamPayload>> events = simulatedTutorService
                .streamReply(session, "霸王龙有什么特点？")
                .collectList()
                .block();

        assertThat(events).isNotNull().isNotEmpty();
        assertThat(events.getFirst().event()).isEqualTo("start");
        assertThat(events.getLast().event()).isEqualTo("done");
        assertThat(events.subList(1, events.size() - 1))
                .allMatch(event -> "delta".equals(event.event()));

        String reconstructed = events.subList(1, events.size() - 1).stream()
                .map(event -> event.data().content())
                .reduce("", String::concat);
        assertThat(reconstructed).isEqualTo(events.getLast().data().content());
        assertThat(reconstructed).contains("小明", "恐龙", "霸王龙有什么特点？");
    }
}
