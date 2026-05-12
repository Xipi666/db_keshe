package pers.luoluo.databasekeshe.logging.service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import pers.luoluo.databasekeshe.logging.dto.RuntimeLogLevel;
import pers.luoluo.databasekeshe.logging.dto.RuntimeLogResponse;
import pers.luoluo.databasekeshe.logging.dto.RuntimeLogSource;

@Service
public class RuntimeLogService {

    private static final int MAX_LOGS = 500;
    private static final int QUERY_LIMIT = 300;

    private final AtomicLong nextId = new AtomicLong(1);
    private final ArrayDeque<RuntimeLogResponse> logs = new ArrayDeque<>();

    public void debug(String message, String context) {
        append(RuntimeLogLevel.DEBUG, message, context);
    }

    public void info(String message, String context) {
        append(RuntimeLogLevel.INFO, message, context);
    }

    public void warn(String message, String context) {
        append(RuntimeLogLevel.WARN, message, context);
    }

    public void error(String message, String context) {
        append(RuntimeLogLevel.ERROR, message, context);
    }

    public List<RuntimeLogResponse> list(RuntimeLogLevel minLevel) {
        RuntimeLogLevel effectiveMinLevel = minLevel == null ? RuntimeLogLevel.INFO : minLevel;
        synchronized (logs) {
            return logs.stream()
                    .filter(log -> weight(log.level()) >= weight(effectiveMinLevel))
                    .sorted(Comparator.comparing(RuntimeLogResponse::createdAt).reversed())
                    .limit(QUERY_LIMIT)
                    .toList();
        }
    }

    private void append(RuntimeLogLevel level, String message, String context) {
        RuntimeLogResponse response = new RuntimeLogResponse(
                nextId.getAndIncrement(),
                RuntimeLogSource.BACKEND,
                level,
                message,
                context,
                LocalDateTime.now()
        );
        synchronized (logs) {
            logs.addFirst(response);
            while (logs.size() > MAX_LOGS) {
                logs.removeLast();
            }
        }
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
