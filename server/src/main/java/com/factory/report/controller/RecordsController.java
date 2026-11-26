package com.factory.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.factory.report.common.PageResult;
import com.factory.report.common.R;
import com.factory.report.entity.BagDaily;
import com.factory.report.entity.LaminationDaily;
import com.factory.report.entity.SysUser;
import com.factory.report.entity.PrintDaily;
import com.factory.report.mapper.BagDailyMapper;
import com.factory.report.mapper.LaminationDailyMapper;
import com.factory.report.mapper.PrintDailyMapper;
import com.factory.report.mapper.SysUserMapper;
import com.factory.report.security.CurrentUser;
import com.factory.report.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 跨部门合并明细视图，见 API.md §7.2。
 * 印刷/复合：required=要求米数、actual=实际米数；制袋：required=来料米数、actual=完成数量（个）。
 */
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordsController {

    private final PrintDailyMapper printMapper;
    private final LaminationDailyMapper lamMapper;
    private final BagDailyMapper bagMapper;
    private final SysUserMapper userMapper;

    @GetMapping
    public R<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String dept,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String machine,
            @RequestParam(required = false) String shift,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean mine) {
        // mine=true：填报端只看本人记录（API.md §6.1 可见范围）
        LoginUser lu = CurrentUser.get();
        Long onlyMine = ("WORKER".equals(lu.getRole()) || mine) ? lu.getId() : null;
        // 主管只看本部门数据（API.md §6.1：主管=本部门）；管理角色不过滤
        String leadDept = lu.isLead() ? lu.getDept() : null;
        String d0 = startDate;
        String d1 = endDate;
        List<Map<String, Object>> all = new ArrayList<>();

        boolean wantPrint = (dept == null || dept.isBlank() || "PRINT".equals(dept))
                && (leadDept == null || "PRINT".equals(leadDept));
        boolean wantLam = (dept == null || dept.isBlank() || "LAMINATION".equals(dept))
                && (leadDept == null || "LAMINATION".equals(leadDept));
        boolean wantBag = (dept == null || dept.isBlank() || "BAGMAKING".equals(dept))
                && (leadDept == null || "BAGMAKING".equals(leadDept));

        if (wantPrint) {
            for (PrintDaily x : printMapper.selectList(new LambdaQueryWrapper<PrintDaily>()
                    .eq(onlyMine != null, PrintDaily::getUserId, onlyMine)
                    .ge(d0 != null, PrintDaily::getDate, d0)
                    .le(d1 != null, PrintDaily::getDate, d1)
                    .like(orderId != null && !orderId.isBlank(), PrintDaily::getOrderId, orderId)
                    .eq(status != null && !status.isBlank(), PrintDaily::getStatus, status))) {
                all.add(row("PRINT", "PRINT", x.getId(), x.getDate(), x.getOrderId(), "1 号印刷机",
                        x.getMaterial(), x.getSpec(), x.getRequiredMeter(), x.getActualMeter(),
                        null, null, x.getStatus(), x.getSigner(), x.getUserId()));
            }
        }
        if (wantLam) {
            for (LaminationDaily x : lamMapper.selectList(new LambdaQueryWrapper<LaminationDaily>()
                    .eq(onlyMine != null, LaminationDaily::getUserId, onlyMine)
                    .ge(d0 != null, LaminationDaily::getDate, d0)
                    .le(d1 != null, LaminationDaily::getDate, d1)
                    .like(orderId != null && !orderId.isBlank(), LaminationDaily::getOrderId, orderId)
                    .eq(status != null && !status.isBlank(), LaminationDaily::getStatus, status))) {
                all.add(row("LAMINATION", "LAMINATION", x.getId(), x.getDate(), x.getOrderId(),
                        "SOLVENT_FREE".equals(x.getMachineType()) ? "无溶剂机" : "干式复合机",
                        x.getMaterial(), x.getSpec(), x.getRequiredMeter(), x.getActualMeter(),
                        x.getRolls(), null, x.getStatus(), x.getSigner(), x.getUserId()));
            }
        }
        if (wantBag) {
            for (BagDaily x : bagMapper.selectList(new LambdaQueryWrapper<BagDaily>()
                    .eq(onlyMine != null, BagDaily::getUserId, onlyMine)
                    .ge(d0 != null, BagDaily::getDate, d0)
                    .le(d1 != null, BagDaily::getDate, d1)
                    .like(orderId != null && !orderId.isBlank(), BagDaily::getOrderId, orderId)
                    .eq(machine != null && !machine.isBlank(), BagDaily::getMachine, machine)
                    .eq(shift != null && !shift.isBlank(), BagDaily::getShift, shift)
                    .eq(status != null && !status.isBlank(), BagDaily::getStatus, status))) {
                all.add(row("BAGMAKING", "BAGMAKING", x.getId(), x.getDate(), x.getOrderId(),
                        x.getMachine(), x.getBagType(), x.getSpec(), x.getIncomingMeter(),
                        x.getCompletedQty() == null ? null : new java.math.BigDecimal(x.getCompletedQty()),
                        null, x.getWasteKg(), x.getStatus(), x.getSigner(), x.getUserId()));
            }
        }

        all.sort(Comparator.comparing((Map<String, Object> m) -> (String) m.get("date")).reversed());

        long total = all.size();
        int from = (int) Math.min((page - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        return R.ok(new PageResult<>(total, all.subList(from, to)));
    }

    private Map<String, Object> row(String module, String dept, Long id, java.time.LocalDate date,
                                    String orderId, String machine, String materialOrBagType, String spec,
                                    Object required, Object actual, Integer rolls,
                                    java.math.BigDecimal wasteKg, String status, String signer, Long userId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("module", module);
        m.put("dept", dept);
        m.put("date", String.valueOf(date));
        m.put("orderId", orderId);
        m.put("machine", machine);
        m.put("materialOrBagType", materialOrBagType);
        m.put("spec", spec);
        m.put("required", required);
        m.put("actual", actual);
        m.put("rolls", rolls);
        m.put("wasteKg", wasteKg);
        m.put("status", status);
        m.put("signer", signer);
        SysUser u = userMapper.selectById(userId);
        m.put("createdBy", u == null ? null : u.getRealName());
        return m;
    }
}
