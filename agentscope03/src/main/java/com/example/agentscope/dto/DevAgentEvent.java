package com.example.agentscope.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DevAgentEvent(
        String type,
        String sessionId,
        String content,
        String eventId,
        String toolCallId,
        String name,
        String state) {

    public static DevAgentEvent session(String sessionId) {
        return new DevAgentEvent("SESSION", sessionId, "", null, null, null, null);
    }

    public static DevAgentEvent message(String sessionId, String eventId, String content) {
        return new DevAgentEvent("MESSAGE", sessionId, content, eventId, null, null, null);
    }

    public static DevAgentEvent agentEvent(
            String type, String sessionId, String eventId, String content) {
        return new DevAgentEvent(type, sessionId, content, eventId, null, null, null);
    }

    public static DevAgentEvent namedEvent(
            String type,
            String sessionId,
            String eventId,
            String toolCallId,
            String name,
            String content) {
        return new DevAgentEvent(type, sessionId, content, eventId, toolCallId, name, null);
    }

    public static DevAgentEvent stateEvent(
            String type,
            String sessionId,
            String eventId,
            String toolCallId,
            String name,
            String state) {
        return new DevAgentEvent(type, sessionId, "", eventId, toolCallId, name, state);
    }

    public static DevAgentEvent done(String sessionId) {
        return new DevAgentEvent("DONE", sessionId, "", null, null, null, null);
    }
}
