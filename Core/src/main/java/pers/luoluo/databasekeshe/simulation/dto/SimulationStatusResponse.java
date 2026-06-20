package pers.luoluo.databasekeshe.simulation.dto;

import java.time.LocalDateTime;

public record SimulationStatusResponse(
        boolean running,
        boolean anomalyEnabled,
        LocalDateTime startedAt,
        LocalDateTime lastWriteAt,
        long writeCount,
        long alarmCount,
        long taskCount,
        int sampleIntervalSeconds
) {
}
