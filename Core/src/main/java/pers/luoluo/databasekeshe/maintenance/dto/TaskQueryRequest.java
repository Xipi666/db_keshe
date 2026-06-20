package pers.luoluo.databasekeshe.maintenance.dto;

import java.time.LocalDateTime;

public record TaskQueryRequest(
        Integer status,
        Long transformerId,
        Long circuitId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String keyword
) {
}
