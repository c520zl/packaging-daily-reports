package com.factory.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 复合部日报 lamination_daily，字段与 API.md §6.3 一致 */
@Data
@TableName("lamination_daily")
public class LaminationDaily {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate date;
    private String orderId;
    private Integer times;
    private BigDecimal speed;
    private String material;
    private String spec;
    private BigDecimal requiredMeter;
    private BigDecimal actualMeter;
    private Integer rolls;
    private BigDecimal printRemain;
    private String machineType;   // DRY/SOLVENT_FREE
    private BigDecimal adjustMeter;
    private String signer;
    private String status;
    private String rejectReason;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
