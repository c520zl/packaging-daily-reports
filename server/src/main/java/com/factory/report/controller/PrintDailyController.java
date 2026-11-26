package com.factory.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.factory.report.common.BusinessException;
import com.factory.report.common.PageResult;
import com.factory.report.common.R;
import com.factory.report.dto.PrintDailyReq;
import com.factory.report.entity.PrintDaily;
import com.factory.report.entity.SysUser;
import com.factory.report.mapper.PrintDailyMapper;
import com.factory.report.mapper.SysUserMapper;
import com.factory.report.security.CurrentUser;
import com.factory.report.security.LoginUser;
import com.factory.report.service.AuditService;
import com.factory.report.service.DailyRules;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 印刷部日报，见 API.md §6（module=PRINT） */
@RestController
@RequestMapping("/api/print-daily")
@RequiredArgsConstructor
public class PrintDailyController {

    public static final String MODULE = "PRINT";
    private final PrintDailyMapper mapper;
    private final SysUserMapper userMapper;
    private final AuditService audit;

    @GetMapping
    public R<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean mine) {
        LoginUser lu = CurrentUser.get();
        LambdaQueryWrapper<PrintDaily> qw = new LambdaQueryWrapper<>();
        // 可见范围：员工只看本人（API.md §6.1）
        if ("WORKER".equals(lu.getRole()) || mine) qw.eq(PrintDaily::getUserId, lu.getId());
        if (date != null && !date.isBlank()) qw.eq(PrintDaily::getDate, date);
        if (startDate != null && !startDate.isBlank()) qw.ge(PrintDaily::getDate, startDate);
        if (endDate != null && !endDate.isBlank()) qw.le(PrintDaily::getDate, endDate);
        if (orderId != null && !orderId.isBlank()) qw.like(PrintDaily::getOrderId, orderId);
        if (status != null && !status.isBlank()) qw.eq(PrintDaily::getStatus, status);
        qw.orderByDesc(PrintDaily::getDate).orderByDesc(PrintDaily::getId);

        Page<PrintDaily> p = mapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> records = p.getRecords().stream().map(this::toVO).toList();
        return R.ok(new PageResult<>(p.getTotal(), records));
    }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody PrintDailyReq req) {
        LoginUser lu = CurrentUser.get();
        checkOrderUnique(req.getDate(), req.getOrderId(), null);
        PrintDaily d = new PrintDaily();
        applyReq(d, req);
        d.setUserId(lu.getId());
        d.setStatus(Boolean.TRUE.equals(req.getSubmit()) ? "PENDING" : "DRAFT");
        if (Boolean.TRUE.equals(req.getSubmit())) DailyRules.validatePrintForSubmit(req);
        if (d.getSigner() == null || d.getSigner().isBlank()) d.setSigner(lu.getRealName());
        try {
            mapper.insert(d);
        } catch (DuplicateKeyException e) {
            // 并发创建唯一索引兜底：统一转业务码 2001（check-then-insert 竞态窗口）
            throw BusinessException.of(2001, "该生产单号在当日已存在");
        }
        audit.log(MODULE, d.getId(), lu, "CREATE", "创建记录 " + d.getOrderId());
        if (Boolean.TRUE.equals(req.getSubmit())) {
            audit.log(MODULE, d.getId(), lu, "SUBMIT", "提交审核");
            notifyLeaders(lu, d);
        }
        return R.ok(toVO(d));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        PrintDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireSee(d.getUserId());
        return R.ok(toVO(d));
    }

    /** 仅 DRAFT / REJECTED 可编辑；REJECTED 编辑后回 DRAFT（API.md §6.1） */
    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @RequestBody PrintDailyReq req) {
        LoginUser lu = CurrentUser.get();
        PrintDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireOwn(d.getUserId());
        if (!"DRAFT".equals(d.getStatus()) && !"REJECTED".equals(d.getStatus())) {
            throw BusinessException.of(2002, "仅草稿或被退回的记录可以修改");
        }
        applyReq(d, req);
        // 用合并后的实体做唯一性检查（req.date 可能为 null，eq(date,null) 会让检查失效）
        checkOrderUnique(d.getDate(), d.getOrderId(), id);
        if ("REJECTED".equals(d.getStatus())) {
            d.setStatus("DRAFT");
            audit.log(MODULE, id, lu, "EDIT", "修改被退回记录，状态回到草稿");
        } else {
            audit.log(MODULE, id, lu, "EDIT", "修改草稿");
        }
        mapper.updateById(d);
        return R.ok(toVO(d));
    }

    /** 员工仅能删本人草稿；主管仅能删本部门草稿/退回单；APPROVED 已锁定禁止删除 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        LoginUser lu = CurrentUser.get();
        PrintDaily d = mapper.selectById(id);
        if (d == null) return R.ok();
        if ("WORKER".equals(lu.getRole())) {
            requireOwn(d.getUserId());
            DailyRules.requireStatus(d.getStatus(), "DRAFT", "删除");
        } else if (lu.isLead()) {
            requireLeadScope(d.getUserId());
            if (!"DRAFT".equals(d.getStatus()) && !"REJECTED".equals(d.getStatus())) {
                throw BusinessException.of(2002, "已审核记录不可删除");
            }
        } else if (lu.isManagement()) {
            if ("APPROVED".equals(d.getStatus())) {
                throw BusinessException.of(2002, "已审核记录不可删除，如需更正请联系管理员");
            }
        }
        mapper.deleteById(id);
        audit.log(MODULE, id, lu, "DELETE", "删除记录 " + d.getOrderId());
        return R.ok();
    }

    @PutMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        LoginUser lu = CurrentUser.get();
        PrintDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireOwn(d.getUserId());
        DailyRules.requireStatus(d.getStatus(), "DRAFT", "提交");
        PrintDailyReq r = toReq(d);
        DailyRules.validatePrintForSubmit(r);
        // CAS 更新：仅 DRAFT → PENDING，防与审核并发时的丢失更新
        int rows = mapper.update(null, new LambdaUpdateWrapper<PrintDaily>()
                .eq(PrintDaily::getId, id)
                .eq(PrintDaily::getStatus, "DRAFT")
                .set(PrintDaily::getStatus, "PENDING"));
        if (rows == 0) throw BusinessException.of(2002, "当前状态不允许提交");
        audit.log(MODULE, id, lu, "SUBMIT", "提交审核");
        notifyLeaders(lu, d);
        return R.ok();
    }

    /** 提交人撤回：PENDING → DRAFT（API.md §6.1）；CAS 防与审核并发 */
    @PutMapping("/{id}/withdraw")
    public R<Void> withdraw(@PathVariable Long id) {
        LoginUser lu = CurrentUser.get();
        PrintDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireOwn(d.getUserId());
        int rows = mapper.update(null, new LambdaUpdateWrapper<PrintDaily>()
                .eq(PrintDaily::getId, id)
                .eq(PrintDaily::getStatus, "PENDING")
                .set(PrintDaily::getStatus, "DRAFT"));
        if (rows == 0) throw BusinessException.of(2002, "当前状态不允许撤回");
        audit.log(MODULE, id, lu, "WITHDRAW", "撤回待审核记录");
        return R.ok();
    }

    /** 主管审核通过：本部门 PENDING → APPROVED 锁定；CAS 防与撤回并发 */
    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id) {
        LoginUser lu = CurrentUser.get();
        PrintDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireLeadScope(d.getUserId());
        int rows = mapper.update(null, new LambdaUpdateWrapper<PrintDaily>()
                .eq(PrintDaily::getId, id)
                .eq(PrintDaily::getStatus, "PENDING")
                .set(PrintDaily::getStatus, "APPROVED"));
        if (rows == 0) throw BusinessException.of(2002, "当前状态不允许审核");
        audit.log(MODULE, id, lu, "APPROVE", "审核通过");
        audit.notify(d.getUserId(), "APPROVED", "日报审核通过",
                "您提交的 " + d.getOrderId() + " 已审核通过并计入汇总", MODULE, id);
        return R.ok();
    }

    /** 主管退回：PENDING → REJECTED，必须填原因；CAS 防与撤回并发 */
    @PutMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        LoginUser lu = CurrentUser.get();
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) throw BusinessException.of(400, "退回必须填写原因");
        PrintDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireLeadScope(d.getUserId());
        int rows = mapper.update(null, new LambdaUpdateWrapper<PrintDaily>()
                .eq(PrintDaily::getId, id)
                .eq(PrintDaily::getStatus, "PENDING")
                .set(PrintDaily::getStatus, "REJECTED")
                .set(PrintDaily::getRejectReason, reason));
        if (rows == 0) throw BusinessException.of(2002, "当前状态不允许退回");
        audit.log(MODULE, id, lu, "REJECT", "退回：" + reason);
        audit.notify(d.getUserId(), "REJECTED", "日报被退回",
                lu.getRealName() + " 退回了 " + d.getOrderId() + "：" + reason, MODULE, id);
        return R.ok();
    }

    @GetMapping("/{id}/timeline")
    public R<List<?>> timeline(@PathVariable Long id) {
        PrintDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireSee(d.getUserId());
        return R.ok(audit.timeline(MODULE, id));
    }

    // ===== helpers =====

    private Map<String, Object> toVO(PrintDaily d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("date", String.valueOf(d.getDate()));
        m.put("orderId", d.getOrderId());
        m.put("onboardTime", d.getOnboardTime() == null ? null : String.valueOf(d.getOnboardTime()));
        m.put("speed", d.getSpeed());
        m.put("material", d.getMaterial());
        m.put("thickness", d.getThickness());
        m.put("spec", d.getSpec());
        m.put("requiredMeter", d.getRequiredMeter());
        m.put("actualMeter", d.getActualMeter());
        m.put("colors", DailyRules.splitColors(d.getColors()));
        m.put("printMinutes", d.getPrintMinutes());
        m.put("signer", d.getSigner());
        m.put("remark", d.getRemark());
        m.put("status", d.getStatus());
        m.put("rejectReason", d.getRejectReason());
        m.put("createdAt", String.valueOf(d.getCreatedAt()));
        SysUser u = userMapper.selectById(d.getUserId());
        m.put("createdBy", u == null ? null : u.getRealName());
        return m;
    }

    private PrintDailyReq toReq(PrintDaily d) {
        PrintDailyReq r = new PrintDailyReq();
        r.setDate(d.getDate());
        r.setOrderId(d.getOrderId());
        r.setOnboardTime(d.getOnboardTime());
        r.setSpeed(d.getSpeed());
        r.setMaterial(d.getMaterial());
        r.setSpec(d.getSpec());
        r.setRequiredMeter(d.getRequiredMeter());
        r.setActualMeter(d.getActualMeter());
        r.setColors(DailyRules.splitColors(d.getColors()));
        r.setSigner(d.getSigner());
        return r;
    }

    private void applyReq(PrintDaily d, PrintDailyReq req) {
        if (req.getDate() != null) d.setDate(req.getDate());
        if (req.getOrderId() != null) d.setOrderId(req.getOrderId());
        if (req.getOnboardTime() != null) d.setOnboardTime(req.getOnboardTime());
        if (req.getSpeed() != null) d.setSpeed(req.getSpeed());
        if (req.getMaterial() != null) d.setMaterial(req.getMaterial());
        if (req.getThickness() != null) d.setThickness(req.getThickness());
        if (req.getSpec() != null) d.setSpec(req.getSpec());
        if (req.getRequiredMeter() != null) d.setRequiredMeter(req.getRequiredMeter());
        if (req.getActualMeter() != null) d.setActualMeter(req.getActualMeter());
        if (req.getColors() != null) d.setColors(DailyRules.joinColors(req.getColors()));
        if (req.getPrintMinutes() != null) d.setPrintMinutes(req.getPrintMinutes());
        if (req.getSigner() != null) d.setSigner(req.getSigner());
        if (req.getRemark() != null) d.setRemark(req.getRemark());
    }

    private void checkOrderUnique(java.time.LocalDate date, String orderId, Long excludeId) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<PrintDaily>()
                .eq(PrintDaily::getDate, date)
                .eq(PrintDaily::getOrderId, orderId)
                .ne(excludeId != null, PrintDaily::getId, excludeId));
        if (count > 0) throw BusinessException.of(2001, "该生产单号在当日已存在");
    }

    private void requireOwn(Long creatorId) {
        LoginUser lu = CurrentUser.get();
        if (!lu.isAdmin() && !lu.getId().equals(creatorId)) {
            throw BusinessException.of(403, "只能操作本人创建的记录");
        }
    }

    private void requireSee(Long creatorId) {
        LoginUser lu = CurrentUser.get();
        SysUser creator = userMapper.selectById(creatorId);
        String dept = creator == null ? null : creator.getDept();
        if (!DailyRules.canSee(lu, creatorId, dept)) {
            throw BusinessException.of(403, "无权查看该记录");
        }
    }

    private void requireLeadScope(Long creatorId) {
        LoginUser lu = CurrentUser.get();
        SysUser creator = userMapper.selectById(creatorId);
        String dept = creator == null ? null : creator.getDept();
        if (!DailyRules.canSee(lu, creatorId, dept)) {
            throw BusinessException.of(403, "无权审核该记录");
        }
    }

    private void notifyLeaders(LoginUser lu, PrintDaily d) {
        List<SysUser> leaders = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "DEPT_LEAD")
                .eq(SysUser::getDept, lu.getDept())
                .eq(SysUser::getEnabled, true));
        for (SysUser leader : leaders) {
            audit.notify(leader.getId(), "SUBMITTED", "新日报待审核",
                    lu.getRealName() + " 提交了 " + d.getOrderId(), MODULE, d.getId());
        }
    }
}
