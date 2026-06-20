package pers.luoluo.databasekeshe.maintenance.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import pers.luoluo.databasekeshe.maintenance.dto.MaintenanceTaskResponse;

@Mapper
public interface MaintenanceTaskMapper {

    @Select("""
            <script>
            SELECT *
            FROM (
                SELECT
                    mt.TASK_ID AS taskId,
                    mt.ALARM_ID AS alarmId,
                    a.TRANSFORMER_ID AS transformerId,
                    bt.NAME AS transformerName,
                    a.CIRCUIT_ID AS circuitId,
                    pc.NAME AS circuitName,
                    a.POINT_ID AS pointId,
                    mp.POINT_NAME AS pointName,
                    mp.POINT_CODE AS pointCode,
                    mp.UNIT AS unit,
                    a.ALARM_TYPE AS alarmType,
                    a.ALARM_LEVEL AS alarmLevel,
                    a.START_VAL AS alarmValue,
                    a.START_TIME AS alarmTime,
                    mt.STATUS AS status,
                    mt.ASSIGNEE AS assignee,
                    mt.FEEDBACK AS feedback,
                    mt.CREATED_AT AS createdAt,
                    mt.UPDATED_AT AS updatedAt,
                    mt.FINISHED_AT AS finishedAt
                FROM MAINT_TASK mt
                JOIN ALARM_LOG a ON a.ID = mt.ALARM_ID
                JOIN BOX_TRANSFORMER bt ON bt.ID = a.TRANSFORMER_ID
                LEFT JOIN POWER_CIRCUIT pc ON pc.ID = a.CIRCUIT_ID
                LEFT JOIN MEASURE_POINT mp ON mp.ID = a.POINT_ID
                WHERE mt.CREATED_AT BETWEEN #{startTime} AND #{endTime}
                <if test="status != null">
                    AND mt.STATUS = #{status}
                </if>
                <if test="transformerId != null">
                    AND a.TRANSFORMER_ID = #{transformerId}
                </if>
                <if test="circuitId != null">
                    AND a.CIRCUIT_ID = #{circuitId}
                </if>
                <if test="keyword != null and keyword != ''">
                    AND (
                        LOWER(bt.NAME) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(pc.NAME) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mp.POINT_NAME) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mp.POINT_CODE) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(a.ALARM_TYPE) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(a.ALARM_LEVEL) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mt.ASSIGNEE) LIKE '%' || LOWER(#{keyword}) || '%'
                        OR LOWER(mt.FEEDBACK) LIKE '%' || LOWER(#{keyword}) || '%'
                    )
                </if>
                ORDER BY mt.CREATED_AT DESC, mt.TASK_ID DESC
            )
            WHERE ROWNUM &lt;= #{limit}
            </script>
            """)
    List<MaintenanceTaskResponse> findTasks(
            @Param("status") Integer status,
            @Param("transformerId") Long transformerId,
            @Param("circuitId") Long circuitId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    @Select("""
            SELECT COUNT(1)
            FROM MAINT_TASK
            WHERE TASK_ID = #{taskId}
            """)
    int existsById(@Param("taskId") Long taskId);

    @Select("""
            SELECT
                mt.TASK_ID AS taskId,
                mt.ALARM_ID AS alarmId,
                a.TRANSFORMER_ID AS transformerId,
                bt.NAME AS transformerName,
                a.CIRCUIT_ID AS circuitId,
                pc.NAME AS circuitName,
                a.POINT_ID AS pointId,
                mp.POINT_NAME AS pointName,
                mp.POINT_CODE AS pointCode,
                mp.UNIT AS unit,
                a.ALARM_TYPE AS alarmType,
                a.ALARM_LEVEL AS alarmLevel,
                a.START_VAL AS alarmValue,
                a.START_TIME AS alarmTime,
                mt.STATUS AS status,
                mt.ASSIGNEE AS assignee,
                mt.FEEDBACK AS feedback,
                mt.CREATED_AT AS createdAt,
                mt.UPDATED_AT AS updatedAt,
                mt.FINISHED_AT AS finishedAt
            FROM MAINT_TASK mt
            JOIN ALARM_LOG a ON a.ID = mt.ALARM_ID
            JOIN BOX_TRANSFORMER bt ON bt.ID = a.TRANSFORMER_ID
            LEFT JOIN POWER_CIRCUIT pc ON pc.ID = a.CIRCUIT_ID
            LEFT JOIN MEASURE_POINT mp ON mp.ID = a.POINT_ID
            WHERE mt.TASK_ID = #{taskId}
            """)
    MaintenanceTaskResponse findById(@Param("taskId") Long taskId);

    @Update("""
            UPDATE MAINT_TASK
            SET
                STATUS = #{status},
                ASSIGNEE = #{assignee},
                FEEDBACK = #{feedback},
                UPDATED_AT = SYSTIMESTAMP,
                FINISHED_AT = CASE
                    WHEN #{status} = 2 THEN COALESCE(FINISHED_AT, SYSTIMESTAMP)
                    ELSE NULL
                END
            WHERE TASK_ID = #{taskId}
            """)
    int updateTask(
            @Param("taskId") Long taskId,
            @Param("status") Integer status,
            @Param("assignee") String assignee,
            @Param("feedback") String feedback
    );
}
