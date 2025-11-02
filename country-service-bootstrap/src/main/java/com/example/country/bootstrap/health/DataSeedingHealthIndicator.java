package com.example.country.bootstrap.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Health indicator that reports UP only when data seeding is complete (if enabled).
 * If data seeding is disabled, this indicator always reports UP.
 */
@Component
public class DataSeedingHealthIndicator implements HealthIndicator {
    
    private final boolean seedingEnabled;
    private final AtomicBoolean seedingComplete = new AtomicBoolean(false);
    private final AtomicReference<String> errorMessage = new AtomicReference<>(null);
    
    public DataSeedingHealthIndicator(@Value("${data.seeding.enabled:false}") boolean seedingEnabled) {
        this.seedingEnabled = seedingEnabled;
        // If seeding is disabled, mark as complete immediately
        if (!seedingEnabled) {
            this.seedingComplete.set(true);
        }
    }
    
    @Override
    public Health health() {
        if (!seedingEnabled) {
            return Health.up()
                    .withDetail("seeding", "disabled")
                    .build();
        }
        
        if (errorMessage.get() != null) {
            return Health.down()
                    .withDetail("seeding", "failed")
                    .withDetail("error", errorMessage.get())
                    .build();
        }
        
        if (seedingComplete.get()) {
            return Health.up()
                    .withDetail("seeding", "complete")
                    .build();
        }
        
        return Health.down()
                .withDetail("seeding", "in-progress")
                .build();
    }
    
    /**
     * Marks seeding as complete. Called by DataSeedingCommandLineRunner when seeding finishes successfully.
     */
    public void markSeedingComplete() {
        this.seedingComplete.set(true);
        this.errorMessage.set(null);
    }
    
    /**
     * Marks seeding as failed. Called by DataSeedingCommandLineRunner when seeding encounters an error.
     */
    public void markSeedingFailed(String error) {
        this.seedingComplete.set(false);
        this.errorMessage.set(error);
    }
}

