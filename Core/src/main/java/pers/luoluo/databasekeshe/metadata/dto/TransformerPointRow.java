package pers.luoluo.databasekeshe.metadata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransformerPointRow(
        Long transformerId,
        String transformerCode,
        String transformerName,
        String transformerType,
        BigDecimal ratedCapacityKva,
        String ratedVoltageRatio,
        LocalDate commissionDate,
        String manufacturer,
        BigDecimal oilLevel,
        String location,
        Integer status,
        Long circuitId,
        String circuitCode,
        String circuitName,
        String direction,
        BigDecimal ratedVoltageKv,
        BigDecimal ratedCurrentA,
        Integer circuitStatus,
        Long pointId,
        String pointCode,
        String pointName,
        String pointGroup,
        String measureType,
        String phaseCode,
        String unit,
        BigDecimal minLimit,
        BigDecimal maxLimit,
        BigDecimal rateLimit
) {
}
