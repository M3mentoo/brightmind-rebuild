package com.brightminds.rebuild.learning.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 500, message = "消息内容不能超过500个字符")
        String message) {
}
