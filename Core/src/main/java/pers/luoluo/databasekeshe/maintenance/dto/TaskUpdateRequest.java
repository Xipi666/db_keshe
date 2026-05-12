package pers.luoluo.databasekeshe.maintenance.dto;

public record TaskUpdateRequest(
        Integer status,
        String assignee,
        String feedback
) {
}
