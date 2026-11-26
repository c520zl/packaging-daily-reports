package com.factory.report.controller;

import com.factory.report.common.BusinessException;
import com.factory.report.common.R;
import com.factory.report.dto.LoginReq;
import com.factory.report.dto.UserVO;
import com.factory.report.entity.SysUser;
import com.factory.report.mapper.SysUserMapper;
import com.factory.report.security.CurrentUser;
import com.factory.report.security.JwtUtil;
import com.factory.report.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 认证接口，见 API.md §2 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginReq req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw BusinessException.of(1001, "用户名或密码错误");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw BusinessException.of(1002, "账号已停用，请联系管理员");
        }
        String token = jwtUtil.sign(user.getId(), user.getUsername(), user.getRole(), user.getDept());
        return R.ok(Map.of(
                "token", token,
                "expiresIn", 86400,
                "user", UserVO.of(user)));
    }

    @GetMapping("/me")
    public R<UserVO> me() {
        LoginUser lu = CurrentUser.get();
        SysUser user = userMapper.selectById(lu.getId());
        return R.ok(UserVO.of(user));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        // 无状态 JWT：前端删除 token 即可；此处留作吊销扩展点
        return R.ok();
    }

    @PutMapping("/password")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        LoginUser lu = CurrentUser.get();
        SysUser user = userMapper.selectById(lu.getId());
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (oldPwd == null || !passwordEncoder.matches(oldPwd, user.getPassword())) {
            throw BusinessException.of(1003, "原密码错误");
        }
        if (newPwd == null || newPwd.length() < 6) {
            throw BusinessException.of(400, "新密码至少 6 位");
        }
        user.setPassword(passwordEncoder.encode(newPwd));
        userMapper.updateById(user);
        return R.ok();
    }
}
