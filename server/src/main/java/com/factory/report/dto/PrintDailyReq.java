package com.factory.report.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 印刷部日报 请求体，字段与 API.md §6.2 一致 */
@Data
public class PrintDailyReq {
    private LocalDate date;
    private String orderId;
    private LocalTime onboardTime;
    private BigDecimal speed;
    private String material;
    private BigDecimal thickness;
    private String spec;
    private BigDecimal requiredMeter;
    private BigDecimal actualMeter;
    private List<String> colors;
    private BigDecimal printMinutes;
    private String signer;
    private String remark;
    /** false=存草稿，true=直接提交进审核（API.md §6.1） */
    private Boolean submit = false;
}
