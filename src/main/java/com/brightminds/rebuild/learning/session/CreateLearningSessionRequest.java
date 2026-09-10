package com.brightminds.rebuild.learning.session;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLearningSessionRequest(
        @NotBlank(message = "儿童姓名不能为空")
        @Size(max = 20, message = "儿童姓名不能超过20个字符")
        String childName,

        @NotNull(message = "儿童年龄不能为空")
        @Min(value = 3, message = "儿童年龄不能小于3岁")
        @Max(value = 8, message = "儿童年龄不能大于8岁")
        Integer childAge,

        @NotBlank(message = "学习主题不能为空")
        @Size(max = 50, message = "学习主题不能超过50个字符")
        String topic) {
}
