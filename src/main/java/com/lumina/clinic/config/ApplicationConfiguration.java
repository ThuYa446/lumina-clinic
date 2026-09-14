package com.lumina.clinic.config;

import com.lumina.clinic.booking.BookingPolicy;
import java.time.Clock;
import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ValidationConfigurationCustomizer validationClock(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock.withZone(BookingPolicy.ZONE));
    }
}
