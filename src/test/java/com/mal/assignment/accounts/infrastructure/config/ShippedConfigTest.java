package com.mal.assignment.accounts.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShippedConfigTest {

    @Test
    void loadsShippedYamlConstants() {
        LedgerConfig config = new YamlLedgerConfigLoader().loadFromClasspath();
        assertEquals(6, config.windowDays());
        assertEquals(RoundingMode.DOWN, config.roundingMode());
        assertEquals(2, config.currencies().get("AED").scale());
        assertEquals(3, config.currencies().get("BHD").scale());
        assertEquals(4, config.interest().dailyRateNumerator());
        assertEquals(10_000, config.interest().dailyRateDenominator());
        assertEquals(6, config.interest().capitalizationDay());
        assertEquals(2500L, config.overdraft().feesInMinorUnits().get("AED"));
        assertEquals(25_000L, config.overdraft().feesInMinorUnits().get("BHD"));
        assertEquals("ACC-001", config.accounts().get(0).id());
        assertEquals("ACC-002", config.accounts().get(1).id());
        assertEquals(0L, config.accounts().get(0).openingBalanceInMinorUnits());
    }
}
