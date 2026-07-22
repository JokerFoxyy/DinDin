package com.poupito.api.common.security;

import com.poupito.api.auth.JwtService;
import com.poupito.api.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			extractToken(request)
					.flatMap(jwtService::extractUserId)
					.flatMap(userRepository::findById)
					.ifPresent(user -> {
						var authentication = new UsernamePasswordAuthenticationToken(
								new AuthenticatedUser(user.getId(), user.getEmail()), null, List.of());
						authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authentication);
					});
		}
		filterChain.doFilter(request, response);
	}

	/** Preferência: cookie httpOnly (navegador). Fallback: header Authorization (clientes de API). */
	private java.util.Optional<String> extractToken(HttpServletRequest request) {
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (AuthCookieFactory.ACCESS_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
					return java.util.Optional.of(cookie.getValue());
				}
			}
		}
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			return java.util.Optional.of(header.substring(BEARER_PREFIX.length()));
		}
		return java.util.Optional.empty();
	}

}
