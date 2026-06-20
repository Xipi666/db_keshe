package pers.luoluo.databasekeshe.query.controller;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pers.luoluo.databasekeshe.query.dto.HistoryDataRow;
import pers.luoluo.databasekeshe.query.dto.HistoryQueryRequest;
import pers.luoluo.databasekeshe.query.dto.MessageCategory;
import pers.luoluo.databasekeshe.query.dto.MessageQueryRequest;
import pers.luoluo.databasekeshe.query.dto.MessageResponse;
import pers.luoluo.databasekeshe.query.service.QueryService;
import pers.luoluo.databasekeshe.security.AccessGuard;
import pers.luoluo.databasekeshe.security.AuthenticatedUser;

@RestController
public class QueryController {

    private final QueryService queryService;
    private final AccessGuard accessGuard;

    public QueryController(QueryService queryService, AccessGuard accessGuard) {
        this.queryService = queryService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/api/messages")
    public List<MessageResponse> messages(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Role-Code") String roleCode,
            @RequestParam(required = false) MessageCategory category,
            @RequestParam(required = false) Long transformerId,
            @RequestParam(required = false) Long circuitId,
            @RequestParam(required = false) Long pointId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String keyword
    ) {
        AuthenticatedUser user = accessGuard.requireUser(userId, roleCode);
        return queryService.queryMessages(user, new MessageQueryRequest(
                category,
                transformerId,
                circuitId,
                pointId,
                startTime,
                endTime,
                keyword
        ));
    }

    @GetMapping("/api/history")
    public List<HistoryDataRow> history(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Role-Code") String roleCode,
            @RequestParam(required = false) Long transformerId,
            @RequestParam(required = false) Long circuitId,
            @RequestParam(required = false) Long pointId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        AuthenticatedUser user = accessGuard.requireUser(userId, roleCode);
        return queryService.queryHistory(user, new HistoryQueryRequest(
                transformerId,
                circuitId,
                pointId,
                startTime,
                endTime
        ));
    }
}
