# BrightMind Rebuild（博观智教复现）

面向 3 至 8 岁儿童的 AI 启蒙教育后端复现项目。当前重点不是堆叠页面，而是重新实现并验证多轮对话、Prompt 编排、SSE 流式输出、模型供应商解耦、异常边界与安全配置。

> 项目仍在迭代中。当前已完成后端文本对话主链路；图像生成、语音和数据库持久化尚未接入。

## 当前能力

- 创建带儿童姓名、年龄和学习主题的学习会话
- Bean Validation 请求校验与统一业务错误码
- 基于 Spring WebFlux 与 SSE 的 `start/delta/done/error` 流式协议
- 年龄化、主题化和儿童安全 Prompt
- 有界内存对话历史，仅在完整响应成功后保存
- `TutorModelClient` 模型抽象
- 默认本地模拟模型
- OpenAI Chat Completions 兼容流式客户端
- 连接超时、流静默超时和受控重试
- 不记录 API Key、完整 Prompt 和儿童原始对话的调用日志

## 核心结构

```text
LearningSessionController
        ↓
TutorChatService
        ├── TutorPromptFactory
        ├── ConversationHistoryStore
        └── TutorModelClient
                ├── SimulatedTutorModelClient（默认）
                └── OpenAiCompatibleTutorModelClient（按配置启用）
```

## 环境

- JDK 21
- Windows PowerShell（其他平台可使用 `./mvnw`）
- 不要求安装全局 Maven

## 本地运行

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

健康检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

创建会话：

```http
POST /api/learning-sessions
Content-Type: application/json

{
  "childName": "小明",
  "childAge": 6,
  "topic": "恐龙"
}
```

使用响应中的 `sessionId` 发起流式消息：

```http
POST /api/learning-sessions/{sessionId}/messages:stream
Content-Type: application/json
Accept: text/event-stream

{
  "message": "霸王龙有什么特点？"
}
```

## SSE 事件

- `start`：建立助手消息
- `delta`：追加模型文本片段
- `done`：响应完整结束
- `error`：流建立后的超时或模型错误

流开始前的参数错误、会话不存在等问题仍返回标准 HTTP 4xx JSON。

## 启用真实兼容模型

默认配置使用模拟模型，不访问互联网。切换真实模型前，需要准备与账号地域匹配的 OpenAI 兼容 `base_url`、API Key 和模型名。

PowerShell 中可隐藏输入 API Key，避免把密钥文字直接写进命令历史：

```powershell
$secureKey = Read-Host 'DASHSCOPE API Key' -AsSecureString
$env:DASHSCOPE_API_KEY = [System.Net.NetworkCredential]::new('', $secureKey).Password
$env:TUTOR_MODEL_BASE_URL = 'https://<WorkspaceId>.cn-beijing.maas.aliyuncs.com/compatible-mode/v1'
$env:TUTOR_MODEL_NAME = 'qwen-plus'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--app.tutor.provider=openai-compatible'
```

不要把真实 API Key 写入源码、`application.properties`、测试、文档或 Git 提交。

## 重试边界

- 只对网络错误、HTTP 429 和 5xx 进行有限重试。
- 默认最多重试 1 次，并使用退避间隔。
- 一旦收到任何模型文本片段，后续错误不再重试，避免重复输出。
- HTTP 401 等配置或权限错误不会重试。

## 测试

```powershell
.\mvnw.cmd test
```

真实模型协议测试使用进程内 Reactor Netty 桩服务，不访问第三方、不需要真实密钥，也不消耗模型额度。

## 当前限制

- 会话与历史仅在内存中，重启后丢失，不支持多实例共享。
- 同一会话的并发提问尚未串行化。
- 尚未验证具体云账号的地域、Workspace、模型权限和额度。
- 尚未实现 token 预算裁剪、数据库、图像、语音和客户端。
