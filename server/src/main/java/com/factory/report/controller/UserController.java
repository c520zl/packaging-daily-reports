package com.factory.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.factory.report.common.BusinessException;
import com.factory.report.common.PageResult;
import com.factory.report.common.R;
import com.factory.report.dto.UserReq;
import com.factory.report.dto.UserVO;
import com.factory.report.entity.SysUser;
import com.factory.report.mapper.SysUserMapper;
import com.factory.report.service.AuditService;
import com.factory.report.security.CurrentUser;
import com.factory.report.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/** 用户管理，见 API.md §3（仅 ADMIN，见 SecurityConfig） */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    private static final Set<String> ROLES = Set.of("ADMIN", "DEPT_LEAD", "WORKER", "MANAGEMENT");
    private static final Set<String> DEPTS = Set.of("PRINT", "LAMINATION", "BAGMAKING");

    @GetMapping
    public R<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "20") long size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String role,
                                      @RequestParam(required = false) String status) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (role != null && !role.isBlank()) qw.eq(SysUser::getRole, role);
        if ("ENABLED".equals(status)) qw.eq(SysUser::getEnabled, true);
        if ("DISABLED".equals(status)) qw.eq(SysUser::getEnabled, false);
        qw.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), qw);
        return R.ok(new PageResult<>(p.getTotal(), p.getRecords().stream().map(UserVO::of).toList()));
    }

    @PostMapping
    public R<UserVO> create(@Valid @RequestBody UserReq req) {
        validateRoleDept(req.getRole(), req.getDept());
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (count > 0) throw BusinessException.of(409, "用户名已存在");
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw BusinessException.of(400, "初始密码不能为空");
        }
        SysUser u = new SysUser();
        apply(u, req);
        if (req.getEnabled() == null) u.setEnabled(true);   // 新增默认启用
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        userMapper.insert(u);
        auditService.log("USER", u.getId(), CurrentUser.get(), "CREATE", "新增用户 " + u.getUsername());
        return R.ok(UserVO.of(u));
    }

    @GetMapping("/{id}")
    public R<UserVO> detail(@PathVariable Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw BusinessException.of(404, "用户不存在");
        return R.ok(UserVO.of(u));
    }

    @PutMapping("/{id}")
    public R<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserReq req) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw BusinessException.of(404, "用户不存在");
        validateRoleDept(req.getRole(), req.getDept());
        // 编辑时 username 不可变（改用户名会撞唯一键且影响登录）
        String originalUsername = u.getUsername();
        apply(u, req);
        u.setUsername(originalUsername);
        if (req.getEnabled() != null) u.setEnabled(req.getEnabled());
        userMapper.updateById(u);
        auditService.log("USER", id, CurrentUser.get(), "EDIT", "编辑用户 " + u.getUsername());
        return R.ok(UserVO.of(u));
    }

    /** 重置密码（API.md §3.3） */
    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw BusinessException.of(404, "用户不存在");
        String newPwd = body.get("newPassword");
        if (newPwd == null || newPwd.length() < 6) throw BusinessException.of(400, "新密码至少 6 位");
        u.setPassword(passwordEncoder.encode(newPwd));
        userMapper.updateById(u);
        auditService.log("USER", id, CurrentUser.get(), "STATUS", "重置密码");
        return R.ok();
    }

    /** 启用/停用（API.md §3.4），停用后 JWT 过滤器立即拒绝该用户 */
    @PutMapping("/{id}/status")
    public R<Void> status(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw BusinessException.of(404, "用户不存在");
        Boolean enabled = body.get("enabled");
        if (enabled == null) throw BusinessException.of(400, "缺少 enabled 参数");
        u.setEnabled(enabled);
        userMapper.updateById(u);
        auditService.log("USER", id, CurrentUser.get(), "STATUS", enabled ? "启用账号" : "停用账号");
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        LoginUser lu = CurrentUser.get();
        if (lu.getId().equals(id)) throw BusinessException.of(409, "不能删除自己");
        SysUser u = userMapper.selectById(id);
        if (u == null) throw BusinessException.of(404, "用户不存在");
        userMapper.deleteById(id);
        auditService.log("USER", id, lu, "DELETE", "删除用户 " + u.getUsername());
        return R.ok();
    }

    private void validateRoleDept(String role, String dept) {
        if (!ROLES.contains(role)) throw BusinessException.of(400, "非法角色");
        if ("DEPT_LEAD".equals(role) || "WORKER".equals(role)) {
            if (dept == null || !DEPTS.contains(dept)) {
                throw BusinessException.of(400, "该角色必须指定部门");
            }
        }
    }

    private void apply(SysUser u, UserReq req) {
        u.setUsername(req.getUsername());
        u.setRealName(req.getRealName());
        u.setGender(req.getGender());
        u.setPhone(req.getPhone());
        u.setEmail(req.getEmail());
        u.setAddress(req.getAddress());
        u.setRole(req.getRole());
        u.setDept(req.getDept());
        u.setEnabled(req.getEnabled());
        u.setAvatarUrl(req.getAvatarUrl());
    }
}
