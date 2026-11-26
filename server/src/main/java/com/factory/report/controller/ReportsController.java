package com.factory.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.factory.report.common.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 报表查询汇总 + Excel 导出，见 API.md §7 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final com.factory.report.mapper.PrintDailyMapper printMapper;
    private final com.factory.report.mapper.LaminationDailyMapper lamMapper;
    private final com.factory.report.mapper.BagDailyMapper bagMapper;

    /** 汇总：只统计 APPROVED（API.md §7.1） */
    @GetMapping("/summary")
    public R<Map<String, Object>> summary(@RequestParam(required = false) String date,
                                          @RequestParam(required = false) String startDate,
                                          @RequestParam(required = false) String endDate,
                                          @RequestParam(required = false) String dept) {
        String d0 = startDate != null ? startDate : date;
        String d1 = endDate != null ? endDate : date;
        Map<String, Object> m = new java.util.HashMap<>();

        if (date != null) {
            m.put("printMeter", sumPrint(date, date));
            m.put("laminationMeter", sumLam(date, date));
            m.put("bagQty", sumBag(date, date));
            m.put("wasteKg", sumWaste(date, date));
        } else {
            m.put("printMeter", sumPrint(d0, d1));
            m.put("laminationMeter", sumLam(d0, d1));
            m.put("bagQty", sumBag(d0, d1));
            m.put("wasteKg", sumWaste(d0, d1));
        }
        return R.ok(m);
    }

    private BigDecimal sumPrint(String d0, String d1) {
        LambdaQueryWrapper<com.factory.report.entity.PrintDaily> qw = new LambdaQueryWrapper<com.factory.report.entity.PrintDaily>()
                .eq(com.factory.report.entity.PrintDaily::getStatus, "APPROVED");
        if (d0 != null) qw.ge(com.factory.report.entity.PrintDaily::getDate, d0);
        if (d1 != null) qw.le(com.factory.report.entity.PrintDaily::getDate, d1);
        return printMapper.selectList(qw).stream()
                .map(com.factory.report.entity.PrintDaily::getActualMeter)
                .filter(x -> x != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumLam(String d0, String d1) {
        LambdaQueryWrapper<com.factory.report.entity.LaminationDaily> qw = new LambdaQueryWrapper<com.factory.report.entity.LaminationDaily>()
                .eq(com.factory.report.entity.LaminationDaily::getStatus, "APPROVED");
        if (d0 != null) qw.ge(com.factory.report.entity.LaminationDaily::getDate, d0);
        if (d1 != null) qw.le(com.factory.report.entity.LaminationDaily::getDate, d1);
        return lamMapper.selectList(qw).stream()
                .map(com.factory.report.entity.LaminationDaily::getActualMeter)
                .filter(x -> x != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long sumBag(String d0, String d1) {
        LambdaQueryWrapper<com.factory.report.entity.BagDaily> qw = new LambdaQueryWrapper<com.factory.report.entity.BagDaily>()
                .eq(com.factory.report.entity.BagDaily::getStatus, "APPROVED");
        if (d0 != null) qw.ge(com.factory.report.entity.BagDaily::getDate, d0);
        if (d1 != null) qw.le(com.factory.report.entity.BagDaily::getDate, d1);
        return bagMapper.selectList(qw).stream()
                .mapToInt(x -> x.getCompletedQty() == null ? 0 : x.getCompletedQty()).sum();
    }

    private BigDecimal sumWaste(String d0, String d1) {
        LambdaQueryWrapper<com.factory.report.entity.BagDaily> qw = new LambdaQueryWrapper<com.factory.report.entity.BagDaily>()
                .eq(com.factory.report.entity.BagDaily::getStatus, "APPROVED");
        if (d0 != null) qw.ge(com.factory.report.entity.BagDaily::getDate, d0);
        if (d1 != null) qw.le(com.factory.report.entity.BagDaily::getDate, d1);
        return bagMapper.selectList(qw).stream()
                .map(com.factory.report.entity.BagDaily::getWasteKg)
                .filter(x -> x != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Excel 导出：跨部门明细（API.md §7.3） */
    @GetMapping("/export")
    public void export(HttpServletResponse resp,
                       @RequestParam(required = false) String dept,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) throws IOException {
        String d0 = startDate;
        String d1 = endDate;
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"部门", "日期", "生产单号", "机台/机型", "材质/袋型", "规格", "要求/来料", "实际/完成", "废料kg", "签名", "状态"});

        if (dept == null || dept.isBlank() || "PRINT".equals(dept)) {
            LambdaQueryWrapper<com.factory.report.entity.PrintDaily> qw = new LambdaQueryWrapper<com.factory.report.entity.PrintDaily>();
            if (d0 != null) qw.ge(com.factory.report.entity.PrintDaily::getDate, d0);
            if (d1 != null) qw.le(com.factory.report.entity.PrintDaily::getDate, d1);
            for (var x : printMapper.selectList(qw)) {
                rows.add(new String[]{"印刷部", String.valueOf(x.getDate()), x.getOrderId(), "印刷机",
                        x.getMaterial(), x.getSpec(), str(x.getRequiredMeter()), str(x.getActualMeter()),
                        "", x.getSigner(), x.getStatus()});
            }
        }
        if (dept == null || dept.isBlank() || "LAMINATION".equals(dept)) {
            LambdaQueryWrapper<com.factory.report.entity.LaminationDaily> qw = new LambdaQueryWrapper<com.factory.report.entity.LaminationDaily>();
            if (d0 != null) qw.ge(com.factory.report.entity.LaminationDaily::getDate, d0);
            if (d1 != null) qw.le(com.factory.report.entity.LaminationDaily::getDate, d1);
            for (var x : lamMapper.selectList(qw)) {
                rows.add(new String[]{"复合部", String.valueOf(x.getDate()), x.getOrderId(),
                        "SOLVENT_FREE".equals(x.getMachineType()) ? "无溶剂机" : "干式复合机",
                        x.getMaterial(), x.getSpec(), str(x.getRequiredMeter()), str(x.getActualMeter()),
                        "", x.getSigner(), x.getStatus()});
            }
        }
        if (dept == null || dept.isBlank() || "BAGMAKING".equals(dept)) {
            LambdaQueryWrapper<com.factory.report.entity.BagDaily> qw = new LambdaQueryWrapper<com.factory.report.entity.BagDaily>();
            if (d0 != null) qw.ge(com.factory.report.entity.BagDaily::getDate, d0);
            if (d1 != null) qw.le(com.factory.report.entity.BagDaily::getDate, d1);
            for (var x : bagMapper.selectList(qw)) {
                rows.add(new String[]{"制袋部", String.valueOf(x.getDate()), x.getOrderId(), x.getMachine(),
                        x.getBagType(), x.getSpec(), str(x.getIncomingMeter()),
                        x.getCompletedQty() == null ? "" : x.getCompletedQty() + "个",
                        str(x.getWasteKg()), x.getSigner(), x.getStatus()});
            }
        }

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("日报表明细");
            for (int i = 0; i < rows.size(); i++) {
                Row r = sheet.createRow(i);
                String[] cells = rows.get(i);
                for (int j = 0; j < cells.length; j++) r.createCell(j).setCellValue(cells[j]);
            }
            String filename = URLEncoder.encode("日报表明细_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8);
            resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
            wb.write(resp.getOutputStream());
        }
    }

    private String str(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }
}
