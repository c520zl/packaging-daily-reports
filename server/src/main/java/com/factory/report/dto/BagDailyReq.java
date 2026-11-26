package com.factory.report.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** 制袋部日报 请求体，字段与 API.md §6.4 一致 */
@Data
public class BagDailyReq {
    private LocalDate date;
    private String shift;
    private String machine;
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
    private Boolean submit = false;
}
