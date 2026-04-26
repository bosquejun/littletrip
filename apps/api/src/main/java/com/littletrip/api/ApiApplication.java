package com.littletrip.api;

/**
 * LittleTrip API Application
 *
 * This is the main entry point for the LittleTrip transit fare API.
 * It provides REST endpoints for processing tap-on/tap-off events
 * and retrieving trip data.
 *
 * @see <a href="https://littletrip.com">LittleTrip</a>
 */
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
