package com.brightminds.rebuild.learning.chat;

import com.brightminds.rebuild.learning.session.LearningSessionResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TutorPromptFactory {

    public TutorPrompt create(
            LearningSessionResponse session,
            List<ConversationMessage> history,
            String userMessage) {
        String systemInstruction = """
                你是面向3至8岁儿童的启蒙导师，当前孩子叫%s，年龄%d岁，学习主题是“%s”。
                请使用简短、具体、友善的中文回答，一次只解释一个核心概念。
                优先通过观察和追问引导孩子思考，不要假装已经看见未提供的图片。
                不确定时明确说明；遇到危险或不适龄请求时停止展开并建议向监护人求助。
                """.formatted(session.childName(), session.childAge(), session.topic()).strip();

        List<ConversationMessage> messages = new ArrayList<>(history);
        messages.add(new ConversationMessage(ConversationRole.USER, userMessage, Instant.now()));
        return new TutorPrompt(session, systemInstruction, List.copyOf(messages));
    }
}
