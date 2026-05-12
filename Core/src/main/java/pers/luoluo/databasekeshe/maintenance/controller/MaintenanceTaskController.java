package pers.luoluo.databasekeshe.maintenance.controller;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pers.luoluo.databasekeshe.maintenance.dto.MaintenanceTaskResponse;
import pers.luoluo.databasekeshe.maintenance.dto.TaskQueryRequest;
import pers.luoluo.databasekeshe.maintenance.dto.TaskUpdateRequest;
import pers.luoluo.databasekeshe.maintenance.service.MaintenanceTaskService;
import pers.luoluo.databasekeshe.security.AccessGuard;
import pers.luoluo.databasekeshe.security.AuthenticatedUser;

@RestController
public class MaintenanceTaskController {

    private final MaintenanceTaskService maintenanceTaskService;
    private final AccessGuard accessGuard;

    public MaintenanceTaskController(MaintenanceTaskService maintenanceTaskService, AccessGuard accessGuard) {
        this.maintenanceTaskService = maintenanceTaskService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/api/tasks")
    public List<MaintenanceTaskResponse> tasks(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Role-Code") String roleCode,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String keyword
    ) {
        AuthenticatedUser user = accessGuard.requireUser(userId, roleCode);
        return maintenanceTaskService.queryTasks(user, new TaskQueryRequest(
                status,
                deviceId,
                startTime,
                endTime,
                keyword
        ));
    }

    @PutMapping("/api/tasks/{taskId}")
    public MaintenanceTaskResponse updateTask(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Role-Code") String roleCode,
            @PathVariable Long taskId,
            @RequestBody TaskUpdateRequest request
    ) {
        AuthenticatedUser user = accessGuard.requireUser(userId, roleCode);
        return maintenanceTaskService.updateTask(user, taskId, request);
    }
}
