package pers.luoluo.databasekeshe.maintenance.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import pers.luoluo.databasekeshe.auth.exception.AuthException;
import pers.luoluo.databasekeshe.maintenance.dto.MaintenanceTaskResponse;
import pers.luoluo.databasekeshe.maintenance.dto.TaskQueryRequest;
import pers.luoluo.databasekeshe.maintenance.dto.TaskUpdateRequest;
import pers.luoluo.databasekeshe.maintenance.mapper.MaintenanceTaskMapper;
import pers.luoluo.databasekeshe.security.AccessGuard;
import pers.luoluo.databasekeshe.security.AuthenticatedUser;
import pers.luoluo.databasekeshe.security.RoleCode;

@Service
public class MaintenanceTaskService {

    private static final int HISTORY_AWARE_TASK_LIMIT = 1000;

    private final MaintenanceTaskMapper maintenanceTaskMapper;
    private final AccessGuard accessGuard;
    private final JdbcTemplate jdbcTemplate;

    public MaintenanceTaskService(MaintenanceTaskMapper maintenanceTaskMapper, AccessGuard accessGuard, JdbcTemplate jdbcTemplate) {
        this.maintenanceTaskMapper = maintenanceTaskMapper;
        this.accessGuard = accessGuard;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MaintenanceTaskResponse> queryTasks(AuthenticatedUser user, TaskQueryRequest request) {
        accessGuard.requireAny(user, RoleCode.ENGINEER, RoleCode.MANAGER);
        validateStatus(request.status());

        LocalDateTime endTime = request.endTime() == null ? LocalDateTime.now() : request.endTime();
        LocalDateTime startTime = request.startTime() == null ? endTime.minusDays(7) : request.startTime();
        validateTimeRange(startTime, endTime);

        return maintenanceTaskMapper.findTasks(
                request.status(),
                request.transformerId(),
                request.circuitId(),
                startTime,
                endTime,
                normalizedKeyword(request.keyword()),
                HISTORY_AWARE_TASK_LIMIT
        );
    }

    public MaintenanceTaskResponse updateTask(AuthenticatedUser user, Long taskId, TaskUpdateRequest request) {
        accessGuard.requireAny(user, RoleCode.ENGINEER);
        if (taskId == null || maintenanceTaskMapper.existsById(taskId) == 0) {
            throw new AuthException(HttpStatus.NOT_FOUND, "Task does not exist.");
        }

        Integer status = request.status();
        validateStatus(status);
        String assignee = normalizedText(request.assignee(), user.displayName());
        String feedback = normalizedText(request.feedback(), null);
        try {
            jdbcTemplate.execute((CallableStatementCreator) (connection) -> {
                var statement = connection.prepareCall("{call PKG_PSM_TASK.UPDATE_TASK(?, ?, ?, ?, ?)}");
                statement.setLong(1, taskId);
                statement.setInt(2, status);
                statement.setString(3, assignee);
                statement.setString(4, feedback);
                statement.setString(5, user.username());
                return statement;
            }, (CallableStatementCallback<Void>) statement -> {
                statement.execute();
                return null;
            });
        } catch (DataAccessException exception) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Failed to update task.");
        }

        MaintenanceTaskResponse updatedTask = maintenanceTaskMapper.findById(taskId);
        if (updatedTask == null) {
            throw new AuthException(HttpStatus.NOT_FOUND, "Task does not exist.");
        }
        return updatedTask;
    }

    private void validateStatus(Integer status) {
        if (status != null && status != 0 && status != 1 && status != 2) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Invalid task status.");
        }
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

    private String normalizedText(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text.trim();
    }
}
