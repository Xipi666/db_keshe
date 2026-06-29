package pers.luoluo.databasekeshe.query.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoryDataRow(
        Long id,
        Long transformerId,
        String transformerName,
        Long circuitId,
        String circuitName,
        Long pointId,
        String pointName,
        String pointCode,
        String unit,
        LocalDateTime sampleTime,
        LocalDateTime rangeEndTime,
        BigDecimal value,
        BigDecimal avgValue,
        BigDecimal minValue,
        BigDecimal maxValue,
        Long sampleCount,
        Integer qualityFlag,
        String granularity,
        LocalDateTime createdAt
) {
}
