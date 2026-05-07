package pers.luoluo.databasekeshe.auth.dto;

public record RegisterRequest(
        String username,
        String password,
        String displayName,
        String roleCode
) {
}
