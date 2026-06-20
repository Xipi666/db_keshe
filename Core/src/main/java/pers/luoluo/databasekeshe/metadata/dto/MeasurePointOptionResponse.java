package pers.luoluo.databasekeshe.metadata.dto;

import java.math.BigDecimal;

public record MeasurePointOptionResponse(
        Long id,
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
