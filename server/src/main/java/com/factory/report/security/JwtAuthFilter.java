package com.factory.report.security;

import com.factory.report.entity.SysUser;
import com.factory.report.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 过滤器：解析 token → 校验用户仍启用（停用立即失效）→ 注入 SecurityContext。
 * 当前用户信息通过 CurrentUser.put(...) 供 Controller 使用。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            Claims claims = jwtUtil.parse(auth.substring(7));
            if (claims != null) {
                Long uid = Long.valueOf(claims.getSubject());
                SysUser user = userMapper.selectById(uid);
                if (user != null && Boolean.TRUE.equals(user.getEnabled())) {
                    LoginUser principal = LoginUser.of(user);
                    CurrentUser.put(principal);
                    var authToken = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        try {
            chain.doFilter(req, resp);
        } finally {
            CurrentUser.clear();
        }
    }
}
