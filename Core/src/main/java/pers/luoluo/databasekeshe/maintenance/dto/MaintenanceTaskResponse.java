package pers.luoluo.databasekeshe.maintenance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MaintenanceTaskResponse(
        Long taskId,
        Long alarmId,
        Long transformerId,
        String transformerName,
        Long circuitId,
        String circuitName,
        Long pointId,
        String pointName,
        String pointCode,
        String unit,
        String alarmType,
        String alarmLevel,
        BigDecimal alarmValue,
        LocalDateTime alarmTime,
        Integer status,
        String assignee,
        String feedback,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime finishedAt
) {
}
