package com.example.agentscope.dto;

public record DevAgentEvent(
        String type,
        String sessionId,
        String content) {

    public static DevAgentEvent session(String sessionId) {
        return new DevAgentEvent("SESSION", sessionId, "");
    }

    public static DevAgentEvent message(String sessionId, String content) {
        return new DevAgentEvent("MESSAGE", sessionId, content);
    }

    public static DevAgentEvent done(String sessionId) {
        return new DevAgentEvent("DONE", sessionId, "");
    }
}
