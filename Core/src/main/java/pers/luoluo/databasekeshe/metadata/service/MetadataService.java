package pers.luoluo.databasekeshe.metadata.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pers.luoluo.databasekeshe.metadata.dto.CircuitOptionResponse;
import pers.luoluo.databasekeshe.metadata.dto.MeasurePointOptionResponse;
import pers.luoluo.databasekeshe.metadata.dto.TransformerOptionResponse;
import pers.luoluo.databasekeshe.metadata.dto.TransformerPointRow;
import pers.luoluo.databasekeshe.metadata.mapper.MetadataMapper;

@Service
public class MetadataService {

    private final MetadataMapper metadataMapper;

    public MetadataService(MetadataMapper metadataMapper) {
        this.metadataMapper = metadataMapper;
    }

    public List<TransformerOptionResponse> listTransformers() {
        Map<Long, TransformerBuilder> transformers = new LinkedHashMap<>();
        for (TransformerPointRow row : metadataMapper.findTransformerPointRows()) {
            TransformerBuilder transformer = transformers.computeIfAbsent(
                    row.transformerId(),
                    ignored -> new TransformerBuilder(row)
            );
            if (row.pointId() == null) {
                continue;
            }

            MeasurePointOptionResponse point = new MeasurePointOptionResponse(
                    row.pointId(),
                    row.pointCode(),
                    row.pointName(),
                    row.pointGroup(),
                    row.measureType(),
                    row.phaseCode(),
                    row.unit(),
                    row.minLimit(),
                    row.maxLimit(),
                    row.rateLimit()
            );

            if (row.circuitId() == null) {
                transformer.points.add(point);
                continue;
            }

            transformer.circuits
                    .computeIfAbsent(row.circuitId(), ignored -> new CircuitBuilder(row))
                    .points
                    .add(point);
        }

        return transformers.values().stream().map(TransformerBuilder::build).toList();
    }

    private static final class TransformerBuilder {

        private final TransformerPointRow row;
        private final Map<Long, CircuitBuilder> circuits = new LinkedHashMap<>();
        private final List<MeasurePointOptionResponse> points = new ArrayList<>();

        private TransformerBuilder(TransformerPointRow row) {
            this.row = row;
        }

        private TransformerOptionResponse build() {
            return new TransformerOptionResponse(
                    row.transformerId(),
                    row.transformerCode(),
                    row.transformerName(),
                    row.transformerType(),
                    row.ratedCapacityKva(),
                    row.ratedVoltageRatio(),
                    row.commissionDate(),
                    row.manufacturer(),
                    row.oilLevel(),
                    row.location(),
                    row.status(),
                    circuits.values().stream().map(CircuitBuilder::build).toList(),
                    points
            );
        }
    }

    private static final class CircuitBuilder {

        private final TransformerPointRow row;
        private final List<MeasurePointOptionResponse> points = new ArrayList<>();

        private CircuitBuilder(TransformerPointRow row) {
            this.row = row;
        }

        private CircuitOptionResponse build() {
            return new CircuitOptionResponse(
                    row.circuitId(),
                    row.circuitCode(),
                    row.circuitName(),
                    row.direction(),
                    row.ratedVoltageKv(),
                    row.ratedCurrentA(),
                    row.circuitStatus(),
                    points
            );
        }
    }
}
