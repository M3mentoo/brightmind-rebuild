package com.brightminds.rebuild.learning.chat;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@ConditionalOnProperty(name = "app.tutor.provider", havingValue = "simulated", matchIfMissing = true)
public class SimulatedTutorModelClient implements TutorModelClient {

    private final TutorProperties properties;

    public SimulatedTutorModelClient(TutorProperties properties) {
        this.properties = properties;
    }

    @Override
    public Flux<String> stream(TutorPrompt prompt) {
        String userMessage = prompt.messages().getLast().content();
        String reply = "%s，我们围绕“%s”来想一想。你问的是“%s”。先观察它的特点，再大胆说出你的答案。"
                .formatted(prompt.session().childName(), prompt.session().topic(), userMessage);
        List<String> deltas = reply.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .toList();
        return Flux.fromIterable(deltas).delayElements(properties.getSimulatedTokenDelay());
    }
}
