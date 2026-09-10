package com.brightminds.rebuild.learning.session;

import java.time.Instant;

public record LearningSessionResponse(
        String sessionId,
        String childName,
        int childAge,
        String topic,
        String status,
        Instant createdAt) {
}
