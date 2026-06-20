package pers.luoluo.databasekeshe.metadata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TransformerOptionResponse(
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
        List<CircuitOptionResponse> circuits,
        List<MeasurePointOptionResponse> points
) {
}
