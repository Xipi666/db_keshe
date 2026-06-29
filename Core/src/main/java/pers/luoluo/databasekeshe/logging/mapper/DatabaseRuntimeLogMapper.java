package pers.luoluo.databasekeshe.logging.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import pers.luoluo.databasekeshe.logging.dto.RuntimeLogResponse;

@Mapper
public interface DatabaseRuntimeLogMapper {

    @Select("""
            <script>
            SELECT *
            FROM (
                SELECT
                    ID AS id,
                    'DATABASE' AS source,
                    LEVEL_CODE AS level,
                    MESSAGE AS message,
                    CONTEXT AS context,
                    CREATED_AT AS createdAt
                FROM DB_RUNTIME_LOG
                WHERE CASE LEVEL_CODE
                    WHEN 'DEBUG' THEN 10
                    WHEN 'INFO' THEN 20
                    WHEN 'WARN' THEN 30
                    WHEN 'ERROR' THEN 40
                END &gt;= #{minWeight}
                ORDER BY CREATED_AT DESC, ID DESC
            )
            WHERE ROWNUM &lt;= #{limit}
            </script>
            """)
    List<RuntimeLogResponse> findLogs(
            @Param("minWeight") int minWeight,
            @Param("limit") int limit
    );
}
