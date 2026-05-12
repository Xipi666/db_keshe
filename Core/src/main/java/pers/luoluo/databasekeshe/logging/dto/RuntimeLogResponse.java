package pers.luoluo.databasekeshe.logging.dto;

import java.time.LocalDateTime;

public record RuntimeLogResponse(
        long id,
        RuntimeLogSource source,
        RuntimeLogLevel level,
        String message,
        String context,
        LocalDateTime createdAt
) {
}
