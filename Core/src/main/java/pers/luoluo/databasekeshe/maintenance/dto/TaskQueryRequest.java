package pers.luoluo.databasekeshe.maintenance.dto;

import java.time.LocalDateTime;

public record TaskQueryRequest(
        Integer status,
        Long deviceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String keyword
) {
}
