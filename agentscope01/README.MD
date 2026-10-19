## 运行main方法后
### 1、创建以下请求
```shell
curl -N -X POST "http://localhost:8080/dev-agent/ask" \
-H "Content-Type:application/json" \
-d '{
"userId":"dev-user-001",
"sessionId":"dev-session-001",
"message":"帮我整理一份今天排查订单接口超时的执行清单"
}'
```
-N 会关闭 curl 的输出缓冲。
终端会先拿到 SESSION，随后持续收到模型文本；请求正常完成时，最后收到 DONE。
模型每次吐出的片段不固定，可能是一个词，也可能只有一两个字。示例里把部分连续片段合并展示：

```text
data:{"type":"SESSION","sessionId":"dev-session-001","content":""}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"1"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"."}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":" "}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"补充"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"并"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"记录"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"超"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"时"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"订单"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"接口"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"的名称"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"发生"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"时间段"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"超"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"时"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"阈值"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"错误"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"码"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"及"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"可用"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"请求"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"样本"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"或"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":" trace"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":" ID"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"。\n"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"2"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"."}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":" "}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"拉"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"取"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"该"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"时间段"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"订单"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"接口"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"的"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"调用"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"量"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"P"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"95"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"/P"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"99"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":" "}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"延迟"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"错误"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"率"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"及"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"限"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"流"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"/"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"熔"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"断"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"触发"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"记录"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"。\n"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"3"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"."}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":" "}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"对比"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"订单"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"接口"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"依赖"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"的"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"数据库"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"缓存"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"消息"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"队列"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"和"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"下游"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"服务"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"在同一"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"时间"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"段的"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"延迟"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"与"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"错误"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"指标"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"。\n"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"4"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"."}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":" "}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"检查"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"应用"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"侧"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"线程"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"池"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"连接"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"池"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"GC"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"CPU"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"内存"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"及"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"慢"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"请求"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"日志"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"。\n"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"5"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"."}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":" "}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"核对"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"当天"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"发布"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"记录"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"配置"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"变更"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"流量"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"峰值"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"和"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"定时"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"任务"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"是否"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"与"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"超"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"时"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"时间"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"窗"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"重合"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"。\n"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"6"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"."}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":" "}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"若"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"信息"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"仍"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"不足"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"，"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"补充"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"监控"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"/"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"日志"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"入口"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"、"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"可"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"复"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"现"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"请求"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"和"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"完整"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"链路"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":" trace"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":" "}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"后"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"继续"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"定位"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"。"}

data:{"type":"DONE","sessionId":"dev-session-001","content":""}
```

模型回答的具体措辞不固定，但方向应该是一份排查清单，而且不能声称已经读取日志、执行命令或定位出具体故障。
再用同一个 userId 和 sessionId 问一句：
```shell
curl -N -X POST "http://localhost:8080/dev-agent/ask" \
 -H "Content-Type:application/json" \
 -d '{
    "userId":"dev-user-001",
    "sessionId":"dev-session-001",
    "message":"我刚才让你整理的是什么任务？一句话回答"
 }'
```
状态正常时，模型会知道上一轮是在整理“订单接口超时”的排查清单。
```text
data:{"type":"SESSION","sessionId":"dev-session-001","content":""}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"你"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"让我"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"整理"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"今天"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"排查"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"订单"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"接口"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"超"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"时的"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"执行"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"清单"}

data:{"type":"MESSAGE","sessionId":"dev-session-001","content":"。"}

data:{"type":"DONE","sessionId":"dev-session-001","content":""}
```

再换一个 sessionId：
```shell
curl -N -X POST "http://localhost:8080/dev-agent/ask" \
 -H "Content-Type:application/json" \
 -d '{
    "userId":"dev-user-001",
    "sessionId":"dev-session-002",
    "message":"我刚才让你整理的是什么任务？不知道就说不知道"
 }'
```
这次它不应该拿到上一段会话里的内容。
```text
data:{"type":"SESSION","sessionId":"dev-session-002","content":""}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"检查"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"当前"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"会话"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"记录"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"："}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"无法"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"确认"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"你"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"此前"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"要求"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"整理"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"的任务"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"，"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"请"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"补充"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"原始"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"问题"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"描述"}

data:{"type":"MESSAGE","sessionId":"dev-session-002","content":"。"}

data:{"type":"DONE","sessionId":"dev-session-002","content":""}
```

这两个测试比只看第一段输出更关键。它能说明 RuntimeContext 和 AgentStateStore 已经参与执行，而不只是把模型返回包装成 SSE。