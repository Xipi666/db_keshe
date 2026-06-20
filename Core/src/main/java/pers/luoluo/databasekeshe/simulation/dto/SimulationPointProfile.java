package pers.luoluo.databasekeshe.simulation.dto;

import java.math.BigDecimal;

public record SimulationPointProfile(
        Long transformerId,
        Long circuitId,
        Long pointId,
        String pointCode,
        String measureType,
        String unit,
        BigDecimal minLimit,
        BigDecimal maxLimit
) {
}
