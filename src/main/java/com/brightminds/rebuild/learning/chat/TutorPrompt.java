package com.brightminds.rebuild.learning.chat;

import com.brightminds.rebuild.learning.session.LearningSessionResponse;
import java.util.List;

public record TutorPrompt(
        LearningSessionResponse session,
        String systemInstruction,
        List<ConversationMessage> messages) {
}
