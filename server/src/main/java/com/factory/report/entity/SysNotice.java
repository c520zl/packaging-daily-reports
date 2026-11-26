package com.factory.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内通知 sys_notice，字段与 API.md §9 一致 */
@Data
@TableName("sys_notice")
public class SysNotice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;         // SUBMITTED/APPROVED/REJECTED
    private String title;
    private String content;
    private String relatedModule;
    private Long relatedId;
    private Boolean readFlag;
    private LocalDateTime createdAt;
}
