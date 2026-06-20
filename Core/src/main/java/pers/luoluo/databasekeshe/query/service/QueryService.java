package pers.luoluo.databasekeshe.query.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import pers.luoluo.databasekeshe.auth.exception.AuthException;
import pers.luoluo.databasekeshe.query.dto.HistoryDataRow;
import pers.luoluo.databasekeshe.query.dto.HistoryQueryRequest;
import pers.luoluo.databasekeshe.query.dto.MessageCategory;
import pers.luoluo.databasekeshe.query.dto.MessageQueryRequest;
import pers.luoluo.databasekeshe.query.dto.MessageResponse;
import pers.luoluo.databasekeshe.query.mapper.QueryMapper;
import pers.luoluo.databasekeshe.security.AccessGuard;
import pers.luoluo.databasekeshe.security.AuthenticatedUser;
import pers.luoluo.databasekeshe.security.RoleCode;

@Service
public class QueryService {

    private static final int MESSAGE_LIMIT = 300;
    private static final int HISTORY_LIMIT = 1000;

    private final QueryMapper queryMapper;
    private final AccessGuard accessGuard;

    public QueryService(QueryMapper queryMapper, AccessGuard accessGuard) {
        this.queryMapper = queryMapper;
        this.accessGuard = accessGuard;
    }

    public List<MessageResponse> queryMessages(AuthenticatedUser user, MessageQueryRequest request) {
        LocalDateTime endTime = request.endTime() == null ? LocalDateTime.now() : request.endTime();
        LocalDateTime startTime = request.startTime() == null ? endTime.minusHours(1) : request.startTime();
        validateTimeRange(startTime, endTime);

        List<MessageCategory> categories = resolveCategories(user, request.category());
        List<MessageResponse> responses = new ArrayList<>();
        for (MessageCategory category : categories) {
            responses.addAll(switch (category) {
                case SAMPLE -> queryMapper.findSampleMessages(
                        request.transformerId(),
                        request.circuitId(),
                        request.pointId(),
                        startTime,
                        endTime,
                        normalizedKeyword(request.keyword()),
                        MESSAGE_LIMIT
                );
                case ALARM -> queryMapper.findAlarmMessages(
                        request.transformerId(),
                        request.circuitId(),
                        request.pointId(),
                        startTime,
                        endTime,
                        normalizedKeyword(request.keyword()),
                        MESSAGE_LIMIT
                );
                case TASK -> queryMapper.findTaskMessages(
                        request.transformerId(),
                        request.circuitId(),
                        request.pointId(),
                        startTime,
                        endTime,
                        normalizedKeyword(request.keyword()),
                        MESSAGE_LIMIT
                );
            });
        }

        return responses.stream()
                .sorted(Comparator.comparing(MessageResponse::eventTime).reversed())
                .limit(MESSAGE_LIMIT)
                .toList();
    }

    public List<HistoryDataRow> queryHistory(AuthenticatedUser user, HistoryQueryRequest request) {
        accessGuard.requireAny(user, RoleCode.OPERATOR, RoleCode.ENGINEER, RoleCode.MANAGER);

        LocalDateTime endTime = request.endTime() == null ? LocalDateTime.now() : request.endTime();
        LocalDateTime startTime = request.startTime() == null ? endTime.minusHours(1) : request.startTime();
        validateTimeRange(startTime, endTime);

        return queryMapper.findHistory(
                request.transformerId(),
                request.circuitId(),
                request.pointId(),
                startTime,
                endTime,
                HISTORY_LIMIT
        );
    }

    private List<MessageCategory> resolveCategories(AuthenticatedUser user, MessageCategory requestedCategory) {
        List<MessageCategory> allowedCategories = switch (user.roleCode()) {
            case ADMIN -> List.of(MessageCategory.SAMPLE, MessageCategory.ALARM, MessageCategory.TASK);
            case OPERATOR -> List.of(MessageCategory.SAMPLE, MessageCategory.ALARM);
            case ENGINEER -> List.of(MessageCategory.ALARM, MessageCategory.TASK);
            case MANAGER -> List.of(MessageCategory.SAMPLE, MessageCategory.ALARM, MessageCategory.TASK);
        };

        if (requestedCategory == null) {
            return allowedCategories;
        }

        if (!allowedCategories.contains(requestedCategory)) {
            throw new AuthException(HttpStatus.FORBIDDEN, "Current role cannot query this message category.");
        }

        return List.of(requestedCategory);
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Start time cannot be after end time.");
        }
    }

    private String normalizedKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
