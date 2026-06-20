package pers.luoluo.databasekeshe.metadata.dto;

import java.math.BigDecimal;
import java.util.List;

public record CircuitOptionResponse(
        Long circuitId,
        String circuitCode,
        String circuitName,
        String direction,
        BigDecimal ratedVoltageKv,
        BigDecimal ratedCurrentA,
        Integer status,
        List<MeasurePointOptionResponse> points
) {
}
