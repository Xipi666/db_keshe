package pers.luoluo.databasekeshe.query.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MessageResponse(
        MessageCategory category,
        Long id,
        Long transformerId,
        String transformerName,
        Long circuitId,
        String circuitName,
        Long pointId,
        String pointName,
        String pointCode,
        LocalDateTime eventTime,
        BigDecimal value,
        String unit,
        Integer qualityFlag,
        String alarmType,
        String alarmLevel,
        Integer status,
        String assignee,
        String feedback
) {
}
