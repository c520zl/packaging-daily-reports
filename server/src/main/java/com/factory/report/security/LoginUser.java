package com.factory.report.security;

import com.factory.report.entity.SysUser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 登录用户主体（放在 ThreadLocal，请求结束清理） */
@Getter
@RequiredArgsConstructor
public class LoginUser {
    private final Long id;
    private final String username;
    private final String realName;
    private final String role;   // ADMIN/DEPT_LEAD/WORKER/MANAGEMENT
    private final String dept;   // PRINT/LAMINATION/BAGMAKING 或 null

    public static LoginUser of(SysUser u) {
        return new LoginUser(u.getId(), u.getUsername(), u.getRealName(), u.getRole(), u.getDept());
    }

    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isLead() { return "DEPT_LEAD".equals(role); }
    public boolean isManagement() { return "MANAGEMENT".equals(role); }
}
