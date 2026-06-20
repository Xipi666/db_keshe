package pers.luoluo.databasekeshe.metadata.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import pers.luoluo.databasekeshe.metadata.dto.TransformerPointRow;

@Mapper
public interface MetadataMapper {

    @Select("""
            SELECT
                bt.ID AS transformerId,
                bt.TRANSFORMER_CODE AS transformerCode,
                bt.NAME AS transformerName,
                bt.TRANSFORMER_TYPE AS transformerType,
                bt.RATED_CAPACITY_KVA AS ratedCapacityKva,
                bt.RATED_VOLTAGE_RATIO AS ratedVoltageRatio,
                bt.COMMISSION_DATE AS commissionDate,
                bt.MANUFACTURER AS manufacturer,
                bt.OIL_LEVEL AS oilLevel,
                bt.LOCATION AS location,
                bt.STATUS AS status,
                pc.ID AS circuitId,
                pc.CIRCUIT_CODE AS circuitCode,
                pc.NAME AS circuitName,
                pc.DIRECTION AS direction,
                pc.RATED_VOLTAGE_KV AS ratedVoltageKv,
                pc.RATED_CURRENT_A AS ratedCurrentA,
                pc.STATUS AS circuitStatus,
                mp.ID AS pointId,
                mp.POINT_CODE AS pointCode,
                mp.POINT_NAME AS pointName,
                mp.POINT_GROUP AS pointGroup,
                mp.MEASURE_TYPE AS measureType,
                mp.PHASE_CODE AS phaseCode,
                mp.UNIT AS unit,
                mp.MIN_LIMIT AS minLimit,
                mp.MAX_LIMIT AS maxLimit,
                mp.RATE_LIMIT AS rateLimit
            FROM BOX_TRANSFORMER bt
            LEFT JOIN MEASURE_POINT mp ON mp.TRANSFORMER_ID = bt.ID
            LEFT JOIN POWER_CIRCUIT pc ON pc.ID = mp.CIRCUIT_ID
            ORDER BY bt.ID, pc.ID NULLS LAST, mp.ID
            """)
    List<TransformerPointRow> findTransformerPointRows();
}
