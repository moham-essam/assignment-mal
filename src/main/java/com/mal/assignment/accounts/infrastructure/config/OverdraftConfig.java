package com.mal.assignment.accounts.infrastructure.config;

import java.util.Map;

public record OverdraftConfig(Map<String, Long> feesInMinorUnits) {
}
