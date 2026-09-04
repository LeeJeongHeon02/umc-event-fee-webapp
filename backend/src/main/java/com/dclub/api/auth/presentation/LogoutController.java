package com.dclub.api.auth.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Ends this application's session for either login provider; does not unlink Kakao. */
@RestController
public class LogoutController {
    private final CsrfLogoutHandler csrfLogoutHandler;

    public LogoutController(CookieCsrfTokenRepository csrfTokenRepository) {
        this.csrfLogoutHandler = new CsrfLogoutHandler(csrfTokenRepository);
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // Invalidate the server session, not just the browser cookie, so an old cookie cannot be reused.
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        new CookieClearingLogoutHandler("JSESSIONID").logout(request, response, authentication);
        csrfLogoutHandler.logout(request, response, authentication);
    }
}
