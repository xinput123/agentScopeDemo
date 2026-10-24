package com.example.agentscope.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * HTTP 请求只保留三个字段
 * @param userId 用户身份, 线上的做法是从登录态、网关 Token 或服务端 Session 里取
 * @param sessionId 会话编号
 * @param message 本次输入
 */
public record DevAgentRequest(
        String userId,
        @NotBlank String sessionId,
        @NotBlank String message) {
}
