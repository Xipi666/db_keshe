package pers.luoluo.databasekeshe.maintenance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MaintenanceTaskResponse(
        Long taskId,
        Long alarmId,
        Long deviceId,
        String deviceName,
        Long tagId,
        String tagName,
        String tagCode,
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
