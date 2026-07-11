package com.dindin.api.common.security;

import com.dindin.api.common.error.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

	@Test
	void shouldAllowAttemptsUpToTheLimit() {
		LoginRateLimiter limiter = new LoginRateLimiter(3, Duration.ofMinutes(1));

		assertThatCode(() -> {
			limiter.check("key");
			limiter.check("key");
			limiter.check("key");
		}).doesNotThrowAnyException();
	}

	@Test
	void shouldThrow_whenLimitExceeded() {
		LoginRateLimiter limiter = new LoginRateLimiter(2, Duration.ofMinutes(1));
		limiter.check("key");
		limiter.check("key");

		assertThatThrownBy(() -> limiter.check("key"))
				.isInstanceOf(TooManyRequestsException.class);
	}

	@Test
	void shouldTrackKeysIndependently() {
		LoginRateLimiter limiter = new LoginRateLimiter(1, Duration.ofMinutes(1));
		limiter.check("a");

		assertThatCode(() -> limiter.check("b")).doesNotThrowAnyException();
	}

	@Test
	void shouldResetAfterWindow() throws InterruptedException {
		LoginRateLimiter limiter = new LoginRateLimiter(1, Duration.ofMillis(40));
		limiter.check("key");
		Thread.sleep(60);

		assertThatCode(() -> limiter.check("key")).doesNotThrowAnyException();
	}

	@Test
	void shouldThrowWithinSameWindow() {
		LoginRateLimiter limiter = new LoginRateLimiter(1, Duration.ofMinutes(5));
		limiter.check("key");

		assertThatThrownBy(() -> limiter.check("key")).isInstanceOf(TooManyRequestsException.class);
		assertThat(true).isTrue();
	}

}
