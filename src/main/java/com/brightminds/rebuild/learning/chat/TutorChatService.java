package com.brightminds.rebuild.learning.chat;

import com.brightminds.rebuild.learning.session.LearningSessionResponse;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TutorChatService {

    private static final Logger LOGGER = Logger.getLogger(TutorChatService.class.getName());

    private final TutorModelClient modelClient;
    private final TutorPromptFactory promptFactory;
    private final ConversationHistoryStore historyStore;
    private final TutorProperties properties;

    public TutorChatService(
            TutorModelClient modelClient,
            TutorPromptFactory promptFactory,
            ConversationHistoryStore historyStore,
            TutorProperties properties) {
        this.modelClient = modelClient;
        this.promptFactory = promptFactory;
        this.historyStore = historyStore;
        this.properties = properties;
    }

    public Flux<ServerSentEvent<ChatStreamPayload>> streamReply(
            LearningSessionResponse session,
            String rawUserMessage) {
        return Flux.defer(() -> createStream(session, rawUserMessage.trim()));
    }

    private Flux<ServerSentEvent<ChatStreamPayload>> createStream(
            LearningSessionResponse session,
            String userMessage) {
        String messageId = UUID.randomUUID().toString();
        TutorPrompt prompt = promptFactory.create(
                session, historyStore.snapshot(session.sessionId()), userMessage);
        StringBuilder completeReply = new StringBuilder();

        ServerSentEvent<ChatStreamPayload> start = event(
                "start", messageId, payload(session.sessionId(), messageId, null, null));

        Flux<ServerSentEvent<ChatStreamPayload>> modelEvents = modelClient.stream(prompt)
                .filter(delta -> delta != null && !delta.isEmpty())
                .switchIfEmpty(Flux.error(new IllegalStateException("Model returned an empty stream")))
                .timeout(properties.getStreamIdleTimeout())
                .map(delta -> {
                    completeReply.append(delta);
                    return event("delta", messageId, payload(session.sessionId(), messageId, delta, null));
                })
                .concatWith(Mono.fromSupplier(() -> {
                    String reply = completeReply.toString();
                    historyStore.appendTurn(session.sessionId(), userMessage, reply);
                    return event("done", messageId, payload(session.sessionId(), messageId, reply, null));
                }))
                .onErrorResume(error -> {
                    boolean timedOut = error instanceof TimeoutException;
                    if (timedOut) {
                        LOGGER.warning("Tutor model stream timed out");
                    } else {
                        LOGGER.log(Level.WARNING, "Tutor model stream failed", error);
                    }
                    String errorCode = timedOut ? "MODEL_TIMEOUT" : "MODEL_STREAM_ERROR";
                    String message = timedOut ? "导师响应超时，请稍后重试" : "导师暂时无法回答，请稍后重试";
                    return Flux.just(event(
                            "error", messageId, payload(session.sessionId(), messageId, message, errorCode)));
                });

        return Flux.concat(Flux.just(start), modelEvents);
    }

    private ChatStreamPayload payload(
            String sessionId,
            String messageId,
            String content,
            String errorCode) {
        return new ChatStreamPayload(sessionId, messageId, content, errorCode);
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
