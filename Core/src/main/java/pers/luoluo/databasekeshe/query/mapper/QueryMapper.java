package pers.luoluo.databasekeshe.query.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import pers.luoluo.databasekeshe.query.dto.HistoryDataRow;
import pers.luoluo.databasekeshe.query.dto.MessageResponse;

@Mapper
public interface QueryMapper {

    @Select("""
            <script>
            SELECT *
            FROM (
                SELECT
                    'SAMPLE' AS category,
                    r.ID AS id,
                    r.TRANSFORMER_ID AS transformerId,
                    bt.NAME AS transformerName,
                    r.CIRCUIT_ID AS circuitId,
                    pc.NAME AS circuitName,
                    r.POINT_ID AS pointId,
                    mp.POINT_NAME AS pointName,
                    mp.POINT_CODE AS pointCode,
                    r.SAMPLE_TIME AS eventTime,
                    r.VAL AS value,
                    mp.UNIT AS unit,
                    r.QUALITY_FLAG AS qualityFlag,
                    NULL AS alarmType,
                    NULL AS alarmLevel,
                    NULL AS status,
                    NULL AS assignee,
                    NULL AS feedback
                FROM TS_RAW_DATA r
                JOIN BOX_TRANSFORMER bt ON bt.ID = r.TRANSFORMER_ID
                LEFT JOIN POWER_CIRCUIT pc ON pc.ID = r.CIRCUIT_ID
                JOIN MEASURE_POINT mp ON mp.ID = r.POINT_ID
                WHERE r.SAMPLE_TIME BETWEEN #{startTime} AND #{endTime}
                <if test="transformerId != null">
                    AND r.TRANSFORMER_ID = #{transformerId}
                </if>
                <if test="circuitId != null">
                    AND r.CIRCUIT_ID = #{circuitId}
                </if>
                <if test="pointId != null">
                    AND r.POINT_ID = #{pointId}
                </if>
                <if test="keyword != null and keyword != ''">
                    AND (
                        LOWER(bt.NAME) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(pc.NAME) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mp.POINT_NAME) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mp.POINT_CODE) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mp.MEASURE_TYPE) LIKE '%' || LOWER(#{keyword}) || '%'
                    )
                </if>
                ORDER BY r.SAMPLE_TIME DESC, r.ID DESC
            )
            WHERE ROWNUM &lt;= #{limit}
            </script>
            """)
    List<MessageResponse> findSampleMessages(
            @Param("transformerId") Long transformerId,
            @Param("circuitId") Long circuitId,
            @Param("pointId") Long pointId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT *
            FROM (
                SELECT
                    'ALARM' AS category,
                    a.ID AS id,
                    a.TRANSFORMER_ID AS transformerId,
                    bt.NAME AS transformerName,
                    a.CIRCUIT_ID AS circuitId,
                    pc.NAME AS circuitName,
                    a.POINT_ID AS pointId,
                    mp.POINT_NAME AS pointName,
                    mp.POINT_CODE AS pointCode,
                    a.START_TIME AS eventTime,
                    a.START_VAL AS value,
                    mp.UNIT AS unit,
                    NULL AS qualityFlag,
                    a.ALARM_TYPE AS alarmType,
                    a.ALARM_LEVEL AS alarmLevel,
                    a.STATUS AS status,
                    NULL AS assignee,
                    NULL AS feedback
                FROM ALARM_LOG a
                JOIN BOX_TRANSFORMER bt ON bt.ID = a.TRANSFORMER_ID
                LEFT JOIN POWER_CIRCUIT pc ON pc.ID = a.CIRCUIT_ID
                LEFT JOIN MEASURE_POINT mp ON mp.ID = a.POINT_ID
                WHERE a.START_TIME BETWEEN #{startTime} AND #{endTime}
                <if test="transformerId != null">
                    AND a.TRANSFORMER_ID = #{transformerId}
                </if>
                <if test="circuitId != null">
                    AND a.CIRCUIT_ID = #{circuitId}
                </if>
                <if test="pointId != null">
                    AND a.POINT_ID = #{pointId}
                </if>
                <if test="keyword != null and keyword != ''">
                    AND (
                        LOWER(bt.NAME) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(pc.NAME) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mp.POINT_NAME) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mp.POINT_CODE) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(a.ALARM_TYPE) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(a.ALARM_LEVEL) LIKE '%' || LOWER(#{keyword}) || '%'
                    )
                </if>
                ORDER BY a.START_TIME DESC, a.ID DESC
            )
            WHERE ROWNUM &lt;= #{limit}
            </script>
            """)
    List<MessageResponse> findAlarmMessages(
            @Param("transformerId") Long transformerId,
            @Param("circuitId") Long circuitId,
            @Param("pointId") Long pointId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT *
            FROM (
                SELECT
                    'TASK' AS category,
                    mt.TASK_ID AS id,
                    a.TRANSFORMER_ID AS transformerId,
                    bt.NAME AS transformerName,
                    a.CIRCUIT_ID AS circuitId,
                    pc.NAME AS circuitName,
                    a.POINT_ID AS pointId,
                    mp.POINT_NAME AS pointName,
                    mp.POINT_CODE AS pointCode,
                    mt.CREATED_AT AS eventTime,
                    a.START_VAL AS value,
                    mp.UNIT AS unit,
                    NULL AS qualityFlag,
                    a.ALARM_TYPE AS alarmType,
                    a.ALARM_LEVEL AS alarmLevel,
                    mt.STATUS AS status,
                    mt.ASSIGNEE AS assignee,
                    mt.FEEDBACK AS feedback
                FROM MAINT_TASK mt
                JOIN ALARM_LOG a ON a.ID = mt.ALARM_ID
                JOIN BOX_TRANSFORMER bt ON bt.ID = a.TRANSFORMER_ID
                LEFT JOIN POWER_CIRCUIT pc ON pc.ID = a.CIRCUIT_ID
                LEFT JOIN MEASURE_POINT mp ON mp.ID = a.POINT_ID
                WHERE mt.CREATED_AT BETWEEN #{startTime} AND #{endTime}
                <if test="transformerId != null">
                    AND a.TRANSFORMER_ID = #{transformerId}
                </if>
                <if test="circuitId != null">
                    AND a.CIRCUIT_ID = #{circuitId}
                </if>
                <if test="pointId != null">
                    AND a.POINT_ID = #{pointId}
                </if>
                <if test="keyword != null and keyword != ''">
                    AND (
                        LOWER(bt.NAME) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(pc.NAME) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mp.POINT_NAME) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mp.POINT_CODE) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mt.ASSIGNEE) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mt.FEEDBACK) LIKE '%' || LOWER(#{keyword}) || '%'
                    )
                </if>
                ORDER BY mt.CREATED_AT DESC, mt.TASK_ID DESC
            )
            WHERE ROWNUM &lt;= #{limit}
            </script>
            """)
    List<MessageResponse> findTaskMessages(
            @Param("transformerId") Long transformerId,
            @Param("circuitId") Long circuitId,
            @Param("pointId") Long pointId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT *
            FROM (
                SELECT
                    H.ID AS id,
                    H.TRANSFORMER_ID AS transformerId,
                    BT.NAME AS transformerName,
                    H.CIRCUIT_ID AS circuitId,
                    PC.NAME AS circuitName,
                    H.POINT_ID AS pointId,
                    MP.POINT_NAME AS pointName,
                    MP.POINT_CODE AS pointCode,
                    MP.UNIT AS unit,
                    H.SAMPLE_TIME AS sampleTime,
                    H.RANGE_END_TIME AS rangeEndTime,
                    H.VALUE AS value,
                    H.AVG_VALUE AS avgValue,
                    H.MIN_VALUE AS minValue,
                    H.MAX_VALUE AS maxValue,
                    H.SAMPLE_COUNT AS sampleCount,
                    H.QUALITY_FLAG AS qualityFlag,
                    H.GRANULARITY AS granularity,
                    H.CREATED_AT AS createdAt
                FROM (
                    SELECT
                        R.ID,
                        R.TRANSFORMER_ID,
                        R.CIRCUIT_ID,
                        R.POINT_ID,
                        R.SAMPLE_TIME,
                        R.SAMPLE_TIME AS RANGE_END_TIME,
                        R.VAL AS VALUE,
                        R.VAL AS AVG_VALUE,
                        R.VAL AS MIN_VALUE,
                        R.VAL AS MAX_VALUE,
                        1 AS SAMPLE_COUNT,
                        R.QUALITY_FLAG,
                        'RAW' AS GRANULARITY,
                        R.CREATED_AT
                    FROM TS_RAW_DATA R
                    WHERE R.SAMPLE_TIME BETWEEN #{startTime} AND #{endTime}
                      AND NOT EXISTS (
                        SELECT 1
                        FROM TS_RAW_DATA_DAY_ARCHIVE A
                        WHERE A.POINT_ID = R.POINT_ID
                          AND A.ARCHIVE_DAY = TRUNC(R.SAMPLE_TIME)
                    )
                    <if test="transformerId != null">
                        AND R.TRANSFORMER_ID = #{transformerId}
                    </if>
                    <if test="circuitId != null">
                        AND R.CIRCUIT_ID = #{circuitId}
                    </if>
                    <if test="pointId != null">
                        AND R.POINT_ID = #{pointId}
                    </if>

                    UNION ALL

                    SELECT
                        A.ID,
                        A.TRANSFORMER_ID,
                        A.CIRCUIT_ID,
                        A.POINT_ID,
                        CAST(A.ARCHIVE_DAY AS TIMESTAMP) AS SAMPLE_TIME,
                        CAST(A.ARCHIVE_DAY + 1 - (1 / 86400) AS TIMESTAMP) AS RANGE_END_TIME,
                        A.DISPLAY_VAL AS VALUE,
                        A.AVG_VAL AS AVG_VALUE,
                        A.MIN_VAL AS MIN_VALUE,
                        A.MAX_VAL AS MAX_VALUE,
                        A.SAMPLE_COUNT,
                        A.MAX_QUALITY_FLAG AS QUALITY_FLAG,
                        'DAILY' AS GRANULARITY,
                        A.CREATED_AT
                    FROM TS_RAW_DATA_DAY_ARCHIVE A
                    WHERE CAST(A.ARCHIVE_DAY AS TIMESTAMP) &lt;= #{endTime}
                      AND CAST(A.ARCHIVE_DAY + 1 AS TIMESTAMP) &gt; #{startTime}
                    <if test="transformerId != null">
                        AND A.TRANSFORMER_ID = #{transformerId}
                    </if>
                    <if test="circuitId != null">
                        AND A.CIRCUIT_ID = #{circuitId}
                    </if>
                    <if test="pointId != null">
                        AND A.POINT_ID = #{pointId}
                    </if>
                ) H
                JOIN BOX_TRANSFORMER BT ON BT.ID = H.TRANSFORMER_ID
                LEFT JOIN POWER_CIRCUIT PC ON PC.ID = H.CIRCUIT_ID
                JOIN MEASURE_POINT MP ON MP.ID = H.POINT_ID
                ORDER BY H.SAMPLE_TIME DESC, H.ID DESC
            )
            WHERE ROWNUM &lt;= #{limit}
            </script>
            """)
    List<HistoryDataRow> findHistory(
            @Param("transformerId") Long transformerId,
            @Param("circuitId") Long circuitId,
            @Param("pointId") Long pointId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit
    );
}
