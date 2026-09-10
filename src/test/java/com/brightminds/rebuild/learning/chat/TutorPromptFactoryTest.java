package com.brightminds.rebuild.learning.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.brightminds.rebuild.learning.session.LearningSessionResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TutorPromptFactoryTest {

    @Test
    void shouldIncludeChildContextHistoryAndSafetyRules() {
        LearningSessionResponse session = new LearningSessionResponse(
                "session-1", "小明", 6, "恐龙", "ACTIVE", Instant.parse("2026-09-10T08:00:00Z"));
        ConversationMessage previous = new ConversationMessage(
                ConversationRole.ASSISTANT, "上一轮回答", Instant.parse("2026-09-10T08:01:00Z"));

        TutorPrompt prompt = new TutorPromptFactory().create(session, List.of(previous), "新的问题");

        assertThat(prompt.systemInstruction()).contains("小明", "6岁", "恐龙", "监护人");
        assertThat(prompt.messages()).hasSize(2);
        assertThat(prompt.messages().getLast().role()).isEqualTo(ConversationRole.USER);
        assertThat(prompt.messages().getLast().content()).isEqualTo("新的问题");
    }
}
