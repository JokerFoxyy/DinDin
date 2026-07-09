package com.financeia.api.auth.dto;

import java.util.UUID;

public record MeResponse(UUID id, String email) {
}
