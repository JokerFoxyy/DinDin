package com.dindin.api.auth.dto;

import java.util.UUID;

public record UserResponse(UUID id, String email) {
}
