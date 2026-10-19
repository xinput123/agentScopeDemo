package com.example.agentscope;

import com.example.agentscope.config.DevAgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 做 AgentScope Java 的第一个 Web 上手，不建议一上来就接 Shell、文件系统、Workspace 和一堆工具。
 * 先把入口搭稳。
 * 后端项目里，真正麻烦的不是让模型回一句话，而是把 Agent 放到 HTTP 接口后面：请求怎么进来，流式结果怎么出去，会话怎么区分，哪些能力暂时不能给模型用。
 *
 * 这里先搭建一条更接近真实后端的例子
 * HTTP 请求
 * -> Controller
 * -> Service
 * -> HarnessAgent
 * -> RuntimeContext(userId, sessionId)
 * -> DeepSeek
 * -> SSE 流式返回
 */
@SpringBootApplication
@EnableConfigurationProperties(DevAgentProperties.class)
public class AgentScopeApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentScopeApplication.class, args);
    }
}


