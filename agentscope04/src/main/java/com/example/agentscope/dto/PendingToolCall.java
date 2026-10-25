package com.example.agentscope.dto;

import java.util.Map;

public record PendingToolCall(
        String toolCallId,
        String name,
        Map<String, Object> input) {
}
