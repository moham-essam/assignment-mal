package com.mal.assignment.accounts.domain.reports;

import java.util.List;

public record DailyReport(int day, List<DailySnapshot> snapshots) {
}
