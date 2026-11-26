package com.factory.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 审计日志 daily_audit_log */
@Data
@TableName("daily_audit_log")
public class DailyAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String module;      // PRINT/LAMINATION/BAGMAKING/USER
    private Long recordId;
    private Long actorId;
    private String actorName;
    private String action;      // CREATE/SUBMIT/WITHDRAW/EDIT/APPROVE/REJECT/DELETE/STATUS
    private String detail;      // 字段级 diff 或原因
    private LocalDateTime createdAt;
}
