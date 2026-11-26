package com.factory.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.factory.report.common.R;
import com.factory.report.entity.BagDaily;
import com.factory.report.entity.LaminationDaily;
import com.factory.report.entity.PrintDaily;
import com.factory.report.entity.SysUser;
import com.factory.report.mapper.BagDailyMapper;
import com.factory.report.mapper.LaminationDailyMapper;
import com.factory.report.mapper.PrintDailyMapper;
import com.factory.report.security.CurrentUser;
import com.factory.report.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 工作台汇总，见 API.md §8；产量只统计 APPROVED */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final PrintDailyMapper printMapper;
    private final LaminationDailyMapper lamMapper;
    private final BagDailyMapper bagMapper;
    private final com.factory.report.mapper.SysUserMapper userMapper;

    @GetMapping("/today")
    public R<Map<String, Object>> today() {
        String today = LocalDate.now().toString();
        List<PrintDaily> prints = printMapper.selectList(new LambdaQueryWrapper<PrintDaily>()
                .eq(PrintDaily::getDate, today));
        List<LaminationDaily> lams = lamMapper.selectList(new LambdaQueryWrapper<LaminationDaily>()
                .eq(LaminationDaily::getDate, today));
        List<BagDaily> bags = bagMapper.selectList(new LambdaQueryWrapper<BagDaily>()
                .eq(BagDaily::getDate, today));

        Map<String, Object> m = new HashMap<>();
        m.put("submittedCount", prints.size() + lams.size() + bags.size());
        m.put("pendingCount", prints.stream().filter(x -> "PENDING".equals(x.getStatus())).count()
                + lams.stream().filter(x -> "PENDING".equals(x.getStatus())).count()
                + bags.stream().filter(x -> "PENDING".equals(x.getStatus())).count());
        m.put("printMeter", sumMeter(prints.stream().filter(x -> "APPROVED".equals(x.getStatus()))
                .map(PrintDaily::getActualMeter).toList()));
        m.put("laminationMeter", sumMeter(lams.stream().filter(x -> "APPROVED".equals(x.getStatus()))
                .map(LaminationDaily::getActualMeter).toList()));
        m.put("bagQty", bags.stream().filter(x -> "APPROVED".equals(x.getStatus()))
                .mapToInt(x -> x.getCompletedQty() == null ? 0 : x.getCompletedQty()).sum());
        m.put("wasteKg", bags.stream().filter(x -> "APPROVED".equals(x.getStatus()))
                .map(BagDaily::getWasteKg).filter(x -> x != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return R.ok(m);
    }

    @GetMapping("/weekly")
    public R<List<Map<String, Object>>> weekly(@RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            String d = LocalDate.now().minusDays(i).toString();
            Map<String, Object> row = new HashMap<>();
            row.put("date", d);
            row.put("printMeter", sumApprovedMeter(printMapper.selectList(
                    new LambdaQueryWrapper<PrintDaily>().eq(PrintDaily::getDate, d)
                            .eq(PrintDaily::getStatus, "APPROVED")),
                    PrintDaily::getActualMeter));
            row.put("laminationMeter", sumApprovedMeter(lamMapper.selectList(
                    new LambdaQueryWrapper<LaminationDaily>().eq(LaminationDaily::getDate, d)
                            .eq(LaminationDaily::getStatus, "APPROVED")),
                    LaminationDaily::getActualMeter));
            row.put("bagQty", bagMapper.selectList(new LambdaQueryWrapper<BagDaily>()
                            .eq(BagDaily::getDate, d).eq(BagDaily::getStatus, "APPROVED"))
                    .stream().mapToInt(x -> x.getCompletedQty() == null ? 0 : x.getCompletedQty()).sum());
            result.add(row);
        }
        return R.ok(result);
    }

    @GetMapping("/pending-count")
    public R<Long> pendingCount() {
        LoginUser lu = CurrentUser.get();
        long count;
        if ("WORKER".equals(lu.getRole())) {
            // 员工：被退回未处理数
            count = printMapper.selectCount(new LambdaQueryWrapper<PrintDaily>()
                    .eq(PrintDaily::getUserId, lu.getId()).eq(PrintDaily::getStatus, "REJECTED"))
                    + lamMapper.selectCount(new LambdaQueryWrapper<LaminationDaily>()
                    .eq(LaminationDaily::getUserId, lu.getId()).eq(LaminationDaily::getStatus, "REJECTED"))
                    + bagMapper.selectCount(new LambdaQueryWrapper<BagDaily>()
                    .eq(BagDaily::getUserId, lu.getId()).eq(BagDaily::getStatus, "REJECTED"));
        } else if ("DEPT_LEAD".equals(lu.getRole())) {
            // 主管：本部门待审数（按创建人部门过滤）
            List<Long> deptUsers = deptUserIds(lu.getDept());
            count = printMapper.selectCount(new LambdaQueryWrapper<PrintDaily>()
                    .in(PrintDaily::getUserId, deptUsers).eq(PrintDaily::getStatus, "PENDING"))
                    + lamMapper.selectCount(new LambdaQueryWrapper<LaminationDaily>()
                    .in(LaminationDaily::getUserId, deptUsers).eq(LaminationDaily::getStatus, "PENDING"))
                    + bagMapper.selectCount(new LambdaQueryWrapper<BagDaily>()
                    .in(BagDaily::getUserId, deptUsers).eq(BagDaily::getStatus, "PENDING"));
        } else {
            count = printMapper.selectCount(new LambdaQueryWrapper<PrintDaily>().eq(PrintDaily::getStatus, "PENDING"))
                    + lamMapper.selectCount(new LambdaQueryWrapper<LaminationDaily>().eq(LaminationDaily::getStatus, "PENDING"))
                    + bagMapper.selectCount(new LambdaQueryWrapper<BagDaily>().eq(BagDaily::getStatus, "PENDING"));
        }
        return R.ok(count);
    }

    private BigDecimal sumMeter(List<BigDecimal> meters) {
        return meters.stream().filter(x -> x != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <T> BigDecimal sumApprovedMeter(List<T> list, java.util.function.Function<T, BigDecimal> f) {
        return list.stream().map(f).filter(x -> x != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Long> deptUserIds(String dept) {
        if (dept == null) return List.of(-1L);
        return userMapper.selectList(new LambdaQueryWrapper<com.factory.report.entity.SysUser>()
                        .eq(com.factory.report.entity.SysUser::getDept, dept)
                        .eq(com.factory.report.entity.SysUser::getEnabled, true))
                .stream().map(com.factory.report.entity.SysUser::getId).toList();
    }
}
