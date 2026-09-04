package com.dclub.api.global.security;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Profile({"dev", "test", "postgres"})
    SecurityFilterChain developmentSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Profile("prod")
    @Order(1)
    SecurityFilterChain productionSecurity(HttpSecurity http,
                                           KakaoOAuth2UserService kakaoOAuth2UserService,
                                           OAuthLoginSuccessHandler successHandler,
                                           ApiSecurityExceptionHandler securityExceptionHandler) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = cookieCsrfTokenRepository();
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/oauth2/**", "/login/**",
                                "/auth/csrf", "/auth/local/**",
                                "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                // API callers always receive the documented Problem JSON instead of an HTML login redirect.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler))
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(kakaoOAuth2UserService))
                        .successHandler(successHandler))
                .build();
    }

    static CookieCsrfTokenRepository cookieCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // The React app runs outside the backend context path (/api/v1), so it must be able to
        // read the CSRF cookie from pages such as /onboarding and mirror it into the request header.
        repository.setCookiePath("/");
        return repository;
    }
}
