package pers.luoluo.databasekeshe.maintenance.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private static final int TASK_LIMIT = 300;

    private final MaintenanceTaskMapper maintenanceTaskMapper;
    private final AccessGuard accessGuard;

    public MaintenanceTaskService(MaintenanceTaskMapper maintenanceTaskMapper, AccessGuard accessGuard) {
        this.maintenanceTaskMapper = maintenanceTaskMapper;
        this.accessGuard = accessGuard;
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
                TASK_LIMIT
        );
    }

    @Transactional
    public MaintenanceTaskResponse updateTask(AuthenticatedUser user, Long taskId, TaskUpdateRequest request) {
        accessGuard.requireAny(user, RoleCode.ENGINEER);
        if (taskId == null || maintenanceTaskMapper.existsById(taskId) == 0) {
            throw new AuthException(HttpStatus.NOT_FOUND, "Task does not exist.");
        }

        Integer status = request.status();
        validateStatus(status);
        String assignee = normalizedText(request.assignee(), user.displayName());
        String feedback = normalizedText(request.feedback(), null);
        maintenanceTaskMapper.updateTask(taskId, status, assignee, feedback);

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
