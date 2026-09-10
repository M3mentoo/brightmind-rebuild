package com.brightminds.rebuild.learning.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConversationHistoryStoreTest {

    @Test
    void shouldEvictWholeOldestTurnWhenHistoryLimitIsReached() {
        TutorProperties properties = new TutorProperties();
        properties.setHistoryMaxMessages(2);
        ConversationHistoryStore store = new ConversationHistoryStore(properties);

        store.appendTurn("session-1", "问题一", "回答一");
        store.appendTurn("session-1", "问题二", "回答二");

        assertThat(store.snapshot("session-1"))
                .extracting(ConversationMessage::content)
                .containsExactly("问题二", "回答二");
    }
}
