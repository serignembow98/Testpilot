package dev.testpilot.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** Horloge injectable : permet des tests deterministes. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
