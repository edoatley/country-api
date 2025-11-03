package com.example.country.bootstrap.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;

import static org.junit.jupiter.api.Assertions.*;

class DataSeedingHealthIndicatorTest {

    private DataSeedingHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        // Tests will create new instances
    }

    @Test
    void shouldReportUpWhenSeedingDisabled() {
        healthIndicator = new DataSeedingHealthIndicator(false);
        
        Health health = healthIndicator.health();
        
        assertEquals(Health.up().build().getStatus(), health.getStatus());
        assertEquals("disabled", health.getDetails().get("seeding"));
    }

    @Test
    void shouldReportDownWhenSeedingInProgress() {
        healthIndicator = new DataSeedingHealthIndicator(true);
        
        Health health = healthIndicator.health();
        
        assertEquals(Health.down().build().getStatus(), health.getStatus());
        assertEquals("in-progress", health.getDetails().get("seeding"));
    }

    @Test
    void shouldReportUpWhenSeedingComplete() {
        healthIndicator = new DataSeedingHealthIndicator(true);
        healthIndicator.markSeedingComplete();
        
        Health health = healthIndicator.health();
        
        assertEquals(Health.up().build().getStatus(), health.getStatus());
        assertEquals("complete", health.getDetails().get("seeding"));
    }

    @Test
    void shouldReportDownWhenSeedingFailed() {
        healthIndicator = new DataSeedingHealthIndicator(true);
        healthIndicator.markSeedingFailed("Table creation failed");
        
        Health health = healthIndicator.health();
        
        assertEquals(Health.down().build().getStatus(), health.getStatus());
        assertEquals("failed", health.getDetails().get("seeding"));
        assertEquals("Table creation failed", health.getDetails().get("error"));
    }

    @Test
    void shouldTransitionFromInProgressToComplete() {
        healthIndicator = new DataSeedingHealthIndicator(true);
        
        Health healthBefore = healthIndicator.health();
        assertEquals(Health.down().build().getStatus(), healthBefore.getStatus());
        assertEquals("in-progress", healthBefore.getDetails().get("seeding"));
        
        healthIndicator.markSeedingComplete();
        
        Health healthAfter = healthIndicator.health();
        assertEquals(Health.up().build().getStatus(), healthAfter.getStatus());
        assertEquals("complete", healthAfter.getDetails().get("seeding"));
    }

    @Test
    void shouldTransitionFromCompleteToFailed() {
        healthIndicator = new DataSeedingHealthIndicator(true);
        healthIndicator.markSeedingComplete();
        
        Health healthBefore = healthIndicator.health();
        assertEquals(Health.up().build().getStatus(), healthBefore.getStatus());
        
        healthIndicator.markSeedingFailed("Unexpected error");
        
        Health healthAfter = healthIndicator.health();
        assertEquals(Health.down().build().getStatus(), healthAfter.getStatus());
        assertEquals("failed", healthAfter.getDetails().get("seeding"));
        assertEquals("Unexpected error", healthAfter.getDetails().get("error"));
    }

    @Test
    void shouldClearErrorWhenMarkedCompleteAfterFailure() {
        healthIndicator = new DataSeedingHealthIndicator(true);
        healthIndicator.markSeedingFailed("Initial error");
        
        Health healthBefore = healthIndicator.health();
        assertTrue(healthBefore.getDetails().containsKey("error"));
        
        healthIndicator.markSeedingComplete();
        
        Health healthAfter = healthIndicator.health();
        assertEquals(Health.up().build().getStatus(), healthAfter.getStatus());
        assertFalse(healthAfter.getDetails().containsKey("error"));
    }
}
