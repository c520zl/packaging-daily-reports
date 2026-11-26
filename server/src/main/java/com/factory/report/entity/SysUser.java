package com.factory.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户表 sys_user，字段与 API.md §3 一致 */
@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String gender;       // M/F
    private String phone;
    private String email;
    private String address;
    private String role;         // ADMIN/DEPT_LEAD/WORKER/MANAGEMENT
    private String dept;         // PRINT/LAMINATION/BAGMAKING
    private String avatarUrl;
    private Boolean enabled;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
