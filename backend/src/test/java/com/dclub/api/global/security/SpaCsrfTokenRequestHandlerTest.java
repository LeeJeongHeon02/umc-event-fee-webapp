package com.dclub.api.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.assertj.core.api.Assertions.assertThat;

class SpaCsrfTokenRequestHandlerTest {

    @Test
    void SPA가_헤더로_보낸_원본_CSRF_토큰을_검증값으로_사용한다() {
        var token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "raw-cookie-token");
        var request = new MockHttpServletRequest();
        request.addHeader(token.getHeaderName(), token.getToken());

        String resolved = new SpaCsrfTokenRequestHandler().resolveCsrfTokenValue(request, token);

        assertThat(resolved).isEqualTo("raw-cookie-token");
    }

    @Test
    void SPA가_모든_페이지에서_CSRF_쿠키를_읽을_수_있도록_루트_경로를_사용한다() {
        var repository = SecurityConfig.cookieCsrfTokenRepository();
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        repository.saveToken(repository.generateToken(request), request, response);

        assertThat(response.getCookie("XSRF-TOKEN")).isNotNull();
        assertThat(response.getCookie("XSRF-TOKEN").getPath()).isEqualTo("/");
    }
}
