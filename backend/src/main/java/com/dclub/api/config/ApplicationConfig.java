package com.dclub.api.config;

import org.springframework.context.annotation.*;
import java.time.Clock;

@Configuration
public class ApplicationConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
