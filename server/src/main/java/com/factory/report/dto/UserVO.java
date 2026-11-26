package com.factory.report.dto;

import lombok.Data;

import java.util.List;

/** 登录响应 user 结构（API.md §2.1），不含 password */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String gender;
    private String phone;
    private String email;
    private String address;
    private String role;
    private String dept;
    private String avatarUrl;
    private Boolean enabled;
    private String createdAt;

    public static UserVO of(com.factory.report.entity.SysUser u) {
        UserVO vo = new UserVO();
        vo.id = u.getId();
        vo.username = u.getUsername();
        vo.realName = u.getRealName();
        vo.gender = u.getGender();
        vo.phone = u.getPhone();
        vo.email = u.getEmail();
        vo.address = u.getAddress();
        vo.role = u.getRole();
        vo.dept = u.getDept();
        vo.avatarUrl = u.getAvatarUrl();
        vo.enabled = u.getEnabled();
        vo.createdAt = u.getCreatedAt() == null ? null : u.getCreatedAt().toLocalDate().toString();
        return vo;
    }
}
