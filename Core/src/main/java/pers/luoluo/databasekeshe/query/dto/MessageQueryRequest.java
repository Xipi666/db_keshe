package pers.luoluo.databasekeshe.query.dto;

import java.time.LocalDateTime;

public record MessageQueryRequest(
        MessageCategory category,
        Long transformerId,
        Long circuitId,
        Long pointId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String keyword
) {
}
