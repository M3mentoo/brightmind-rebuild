package com.brightminds.rebuild.learning.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.brightminds.rebuild.learning.session.LearningSessionResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulatedTutorModelClientTest {

    @Test
    void shouldStreamAContextualReply() {
        TutorProperties properties = new TutorProperties();
        properties.setSimulatedTokenDelay(Duration.ZERO);
        SimulatedTutorModelClient client = new SimulatedTutorModelClient(properties);
        LearningSessionResponse session = new LearningSessionResponse(
                "session-1", "小明", 6, "恐龙", "ACTIVE", Instant.parse("2026-09-10T08:00:00Z"));
        TutorPrompt prompt = new TutorPrompt(
                session,
                "system",
                List.of(new ConversationMessage(
                        ConversationRole.USER, "霸王龙有什么特点？", Instant.parse("2026-09-10T08:01:00Z"))));

        String reply = client.stream(prompt).collectList().map(parts -> String.join("", parts)).block();

        assertThat(reply).contains("小明", "恐龙", "霸王龙有什么特点？");
    }
}
