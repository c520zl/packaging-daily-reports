package com.factory.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 印刷部日报 print_daily，字段与 API.md §6.2 一致 */
@Data
@TableName("print_daily")
public class PrintDaily {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate date;
    private String orderId;
    private LocalTime onboardTime;
    private BigDecimal speed;
    private String material;
    private BigDecimal thickness;
    private String spec;
    private BigDecimal requiredMeter;
    private BigDecimal actualMeter;
    private String colors;        // 逗号分隔
    private BigDecimal printMinutes;
    private String signer;
    private String remark;
    private String status;        // DRAFT/PENDING/APPROVED/REJECTED
    private String rejectReason;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
