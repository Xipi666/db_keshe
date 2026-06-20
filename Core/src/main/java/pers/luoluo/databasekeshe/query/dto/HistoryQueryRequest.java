package pers.luoluo.databasekeshe.query.dto;

import java.time.LocalDateTime;

public record HistoryQueryRequest(
        Long transformerId,
        Long circuitId,
        Long pointId,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
