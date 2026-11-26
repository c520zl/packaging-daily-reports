package com.factory.report.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 复合部日报 请求体，字段与 API.md §6.3 一致 */
@Data
public class LaminationDailyReq {
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
    private String machineType;
    private BigDecimal adjustMeter;
    private String signer;
    private Boolean submit = false;
}
