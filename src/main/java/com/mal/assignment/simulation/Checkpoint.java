package com.mal.assignment.simulation;

import java.util.Map;

public record Checkpoint(String name, Map<String, Long> amountByAccountId) {
}
