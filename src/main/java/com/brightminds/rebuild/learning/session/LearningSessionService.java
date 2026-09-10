package com.brightminds.rebuild.learning.session;

import com.brightminds.rebuild.common.exception.BusinessException;
import com.brightminds.rebuild.common.exception.ErrorCode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class LearningSessionService {

    private final ConcurrentMap<String, LearningSessionResponse> activeSessions = new ConcurrentHashMap<>();

    public LearningSessionResponse create(CreateLearningSessionRequest request) {
        String childName = request.childName().trim();
        String topic = request.topic().trim();
        String uniquenessKey = normalize(childName) + ":" + normalize(topic);

        LearningSessionResponse session = new LearningSessionResponse(
                UUID.randomUUID().toString(),
                childName,
                request.childAge(),
                topic,
                "ACTIVE",
                Instant.now());

        LearningSessionResponse existing = activeSessions.putIfAbsent(uniquenessKey, session);
        if (existing != null) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_EXISTS);
        }
        return session;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
