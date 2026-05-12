package pers.luoluo.databasekeshe.simulation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pers.luoluo.databasekeshe.security.AccessGuard;
import pers.luoluo.databasekeshe.security.AuthenticatedUser;
import pers.luoluo.databasekeshe.security.RoleCode;
import pers.luoluo.databasekeshe.simulation.dto.AnomalyToggleRequest;
import pers.luoluo.databasekeshe.simulation.dto.SimulationStatusResponse;
import pers.luoluo.databasekeshe.simulation.service.SimulationService;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;
    private final AccessGuard accessGuard;

    public SimulationController(SimulationService simulationService, AccessGuard accessGuard) {
        this.simulationService = simulationService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/status")
    public SimulationStatusResponse status(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Role-Code") String roleCode
    ) {
        requireAdmin(userId, roleCode);
        return simulationService.status();
    }

    @PostMapping("/start")
    public SimulationStatusResponse start(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Role-Code") String roleCode
    ) {
        requireAdmin(userId, roleCode);
        return simulationService.start();
    }

    @PostMapping("/stop")
    public SimulationStatusResponse stop(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Role-Code") String roleCode
    ) {
        requireAdmin(userId, roleCode);
        return simulationService.stop();
    }

    @PutMapping("/anomaly")
    public SimulationStatusResponse anomaly(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Role-Code") String roleCode,
            @RequestBody AnomalyToggleRequest request
    ) {
        requireAdmin(userId, roleCode);
        return simulationService.setAnomalyEnabled(request.enabled());
    }

    private AuthenticatedUser requireAdmin(Long userId, String roleCode) {
        AuthenticatedUser user = accessGuard.requireUser(userId, roleCode);
        accessGuard.requireAny(user, RoleCode.ADMIN);
        return user;
    }
}
