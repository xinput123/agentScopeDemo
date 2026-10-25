package com.example.agentscope.dto;

import jakarta.validation.constraints.NotBlank;

public record DevAgentConfirmRequest(
        String userId,
        @NotBlank String sessionId,
        boolean approved) {
}
