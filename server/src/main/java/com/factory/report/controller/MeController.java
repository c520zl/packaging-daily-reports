package com.factory.report.controller;

import com.factory.report.common.R;
import com.factory.report.dto.UserVO;
import com.factory.report.entity.SysUser;
import com.factory.report.mapper.SysUserMapper;
import com.factory.report.security.CurrentUser;
import com.factory.report.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** 个人资料自助维护，见 API.md §4；头像存储于 RustFS 对象存储 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final SysUserMapper userMapper;
    private final FileStorage fileStorage;

    @GetMapping("/profile")
    public R<UserVO> profile() {
        return R.ok(UserVO.of(userMapper.selectById(CurrentUser.get().getId())));
    }

    /** username/role/dept 只读（API.md §4） */
    @PutMapping("/profile")
    public R<UserVO> updateProfile(@RequestBody Map<String, String> body) {
        SysUser u = userMapper.selectById(CurrentUser.get().getId());
        if (body.containsKey("realName")) u.setRealName(body.get("realName"));
        if (body.containsKey("gender")) u.setGender(body.get("gender"));
        if (body.containsKey("phone")) u.setPhone(body.get("phone"));
        if (body.containsKey("email")) u.setEmail(body.get("email"));
        if (body.containsKey("address")) u.setAddress(body.get("address"));
        userMapper.updateById(u);
        return R.ok(UserVO.of(u));
    }

    @PostMapping("/avatar")
    public R<Map<String, String>> avatar(@RequestParam("file") MultipartFile file) throws Exception {
        String original = file.getOriginalFilename();
        if (original == null || !(original.toLowerCase().endsWith(".jpg") || original.toLowerCase().endsWith(".jpeg") || original.toLowerCase().endsWith(".png"))) {
            return R.error(400, "仅支持 JPG / PNG");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return R.error(400, "图片不能超过 2MB");
        }
        String url = fileStorage.saveAvatar(file, original);
        SysUser u = userMapper.selectById(CurrentUser.get().getId());
        u.setAvatarUrl(url);
        userMapper.updateById(u);
        return R.ok(Map.of("avatarUrl", url));
    }
}
