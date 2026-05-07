package pers.luoluo.databasekeshe.auth.dto;

import pers.luoluo.databasekeshe.auth.domain.SysUser;

public record AuthResponse(
        Long id,
        String username,
        String displayName,
        String roleCode
) {
    public static AuthResponse from(SysUser user) {
        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRoleCode()
        );
    }
}
