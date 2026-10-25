package com.example.agentscope.dto;

import jakarta.validation.constraints.NotBlank;

public record DevAgentRequest(
        String userId,
        @NotBlank String sessionId,
        @NotBlank String message) {
}
