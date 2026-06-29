package pers.luoluo.databasekeshe.logging.service;

import java.util.List;
import org.springframework.stereotype.Service;
import pers.luoluo.databasekeshe.logging.dto.RuntimeLogLevel;
import pers.luoluo.databasekeshe.logging.dto.RuntimeLogResponse;
import pers.luoluo.databasekeshe.logging.mapper.DatabaseRuntimeLogMapper;

@Service
public class DatabaseRuntimeLogService {

    private static final int QUERY_LIMIT = 300;

    private final DatabaseRuntimeLogMapper databaseRuntimeLogMapper;

    public DatabaseRuntimeLogService(DatabaseRuntimeLogMapper databaseRuntimeLogMapper) {
        this.databaseRuntimeLogMapper = databaseRuntimeLogMapper;
    }

    public List<RuntimeLogResponse> list(RuntimeLogLevel minLevel) {
        return databaseRuntimeLogMapper.findLogs(weight(minLevel == null ? RuntimeLogLevel.INFO : minLevel), QUERY_LIMIT);
    }

    private int weight(RuntimeLogLevel level) {
        return switch (level) {
            case DEBUG -> 10;
            case INFO -> 20;
            case WARN -> 30;
            case ERROR -> 40;
        };
    }
}
