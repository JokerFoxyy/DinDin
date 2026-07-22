package com.guaranin.api.common.security;

import java.util.UUID;

/** Principal autenticado colocado no SecurityContext pelo JwtAuthFilter. */
public record AuthenticatedUser(UUID id, String email) {
}
