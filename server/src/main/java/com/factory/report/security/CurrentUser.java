package com.factory.report.security;

public final class CurrentUser {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void put(LoginUser user) { HOLDER.set(user); }
    public static LoginUser get() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw com.factory.report.common.BusinessException.of(401, "未登录或登录已过期");
        }
        return u;
    }
    public static void clear() { HOLDER.remove(); }

    private CurrentUser() {}
}
