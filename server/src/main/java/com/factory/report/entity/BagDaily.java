package com.factory.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 制袋部日报 bag_daily，字段与 API.md §6.4 一致 */
@Data
@TableName("bag_daily")
public class BagDaily {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate date;
    private String shift;          // DAY/NIGHT
    private String machine;        // 1号机..8号机
    private String picker;
    private String leader;
    private BigDecimal speed;
    private String orderId;
    private String bagType;
    private String spec;
    private BigDecimal incomingMeter;
    private BigDecimal adjustMeter;
    private BigDecimal endMeter;
    private Integer completedQty;
    private BigDecimal wasteKg;
    private BigDecimal remainKg;
    private LocalTime workStart;
    private LocalTime workEnd;
    private String abnormalReason;
    private Integer defectQty;
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
