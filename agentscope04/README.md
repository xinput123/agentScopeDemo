# AgentScope Java 2.0 Permission 权限实战

这个分支演示 AgentScope Java 2.0 的工具权限和人工确认流程。
只读项目工具可以直接执行；写入 `notes/` 目录前会暂停并等待确认；删除文件
会被直接拒绝。

配置模型密钥并启动应用：

```bash
export DEEPSEEK_API_KEY=<your-api-key>
mvn spring-boot:run
```

```bash
curl -N -X POST "http://localhost:8080/dev-agent/ask" \
  -H "Content-Type: application/json" \
  -d '{"userId":"dev-user-001","sessionId":"permission-session-001","message":"请创建 notes/permission-demo.txt，内容是：AgentScope Permission HITL 已通过。"}'
```

使用相同的 `userId` 和 `sessionId` 批准待确认的工具调用：

```bash
curl -N -X POST "http://localhost:8080/dev-agent/confirm" \
  -H "Content-Type: application/json" \
  -d '{"userId":"dev-user-001","sessionId":"permission-write-001","approved":true}'
```

批准后，Agent 会写入 `notes/permission-demo.txt`。删除该文件的请求不会执行。
