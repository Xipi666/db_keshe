package pers.luoluo.databasekeshe.logging.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pers.luoluo.databasekeshe.logging.dto.RuntimeLogLevel;
import pers.luoluo.databasekeshe.logging.dto.RuntimeLogResponse;
import pers.luoluo.databasekeshe.logging.service.DatabaseRuntimeLogService;
import pers.luoluo.databasekeshe.security.AccessGuard;
import pers.luoluo.databasekeshe.security.AuthenticatedUser;
import pers.luoluo.databasekeshe.security.RoleCode;

@RestController
public class RuntimeLogController {

    private final DatabaseRuntimeLogService runtimeLogService;
    private final AccessGuard accessGuard;

    public RuntimeLogController(DatabaseRuntimeLogService runtimeLogService, AccessGuard accessGuard) {
        this.runtimeLogService = runtimeLogService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/api/runtime-logs")
    public List<RuntimeLogResponse> logs(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Role-Code") String roleCode,
            @RequestParam(defaultValue = "INFO") RuntimeLogLevel level
    ) {
        AuthenticatedUser user = accessGuard.requireUser(userId, roleCode);
        accessGuard.requireAny(user, RoleCode.ADMIN);
        return runtimeLogService.list(level);
    }
}
