package com.factory.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增/编辑用户 请求体（API.md §3.2） */
@Data
public class UserReq {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50)
    private String username;
    private String password;      // 新增必填，编辑不传
    @NotBlank(message = "姓名不能为空")
    private String realName;
    @Pattern(regexp = "^[MF]?$", message = "性别只能是 M 或 F")
    private String gender;
    private String phone;
    private String email;
    private String address;
    @NotBlank(message = "角色不能为空")
    private String role;
    private String dept;
    /** 编辑时不传则保持原值（null = 不修改），避免编辑资料把已停用账号悄悄重新启用 */
    private Boolean enabled;
    private String avatarUrl;
}
