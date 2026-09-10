package com.brightminds.rebuild.learning.chat;

import com.brightminds.rebuild.learning.session.LearningSessionResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class SimulatedTutorService {

    private static final Duration DEFAULT_TOKEN_DELAY = Duration.ofMillis(35);

    private final Duration tokenDelay;

    public SimulatedTutorService() {
        this(DEFAULT_TOKEN_DELAY);
    }

    SimulatedTutorService(Duration tokenDelay) {
        this.tokenDelay = tokenDelay;
    }

    public Flux<ServerSentEvent<ChatStreamPayload>> streamReply(
            LearningSessionResponse session,
            String userMessage) {
        String messageId = UUID.randomUUID().toString();
        String reply = buildReply(session, userMessage.trim());
        List<String> deltas = reply.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .toList();

        ServerSentEvent<ChatStreamPayload> start = event(
                "start", messageId, new ChatStreamPayload(session.sessionId(), messageId, null));

        Flux<ServerSentEvent<ChatStreamPayload>> tokenEvents = Flux.fromIterable(deltas)
                .delayElements(tokenDelay)
                .map(delta -> event(
                        "delta", messageId, new ChatStreamPayload(session.sessionId(), messageId, delta)));

        ServerSentEvent<ChatStreamPayload> done = event(
                "done", messageId, new ChatStreamPayload(session.sessionId(), messageId, reply));

        return Flux.concat(Flux.just(start), tokenEvents, Flux.just(done));
    }

    private String buildReply(LearningSessionResponse session, String userMessage) {
        return "%s，我们围绕“%s”来想一想。你问的是“%s”。先观察它的特点，再大胆说出你的答案。"
                .formatted(session.childName(), session.topic(), userMessage);
    }

    private ServerSentEvent<ChatStreamPayload> event(
            String eventName,
            String messageId,
            ChatStreamPayload payload) {
        return ServerSentEvent.<ChatStreamPayload>builder()
                .id(messageId)
                .event(eventName)
                .data(payload)
                .build();
    }
}
