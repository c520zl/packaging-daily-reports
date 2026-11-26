package com.factory.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.factory.report.common.BusinessException;
import com.factory.report.common.PageResult;
import com.factory.report.common.R;
import com.factory.report.dto.LaminationDailyReq;
import com.factory.report.entity.LaminationDaily;
import com.factory.report.entity.SysUser;
import com.factory.report.mapper.LaminationDailyMapper;
import com.factory.report.mapper.SysUserMapper;
import com.factory.report.security.CurrentUser;
import com.factory.report.security.LoginUser;
import com.factory.report.service.AuditService;
import com.factory.report.service.DailyRules;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 复合部日报，见 API.md §6（module=LAMINATION），状态机与印刷部一致 */
@RestController
@RequestMapping("/api/lamination-daily")
@RequiredArgsConstructor
public class LaminationDailyController {

    public static final String MODULE = "LAMINATION";
    private static final Set<String> MACHINE_TYPES = Set.of("DRY", "SOLVENT_FREE");

    private final LaminationDailyMapper mapper;
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
            @RequestParam(required = false) String machineType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean mine) {
        LoginUser lu = CurrentUser.get();
        LambdaQueryWrapper<LaminationDaily> qw = new LambdaQueryWrapper<>();
        if ("WORKER".equals(lu.getRole()) || mine) qw.eq(LaminationDaily::getUserId, lu.getId());
        if (date != null && !date.isBlank()) qw.eq(LaminationDaily::getDate, date);
        if (startDate != null && !startDate.isBlank()) qw.ge(LaminationDaily::getDate, startDate);
        if (endDate != null && !endDate.isBlank()) qw.le(LaminationDaily::getDate, endDate);
        if (orderId != null && !orderId.isBlank()) qw.like(LaminationDaily::getOrderId, orderId);
        if (machineType != null && !machineType.isBlank()) qw.eq(LaminationDaily::getMachineType, machineType);
        if (status != null && !status.isBlank()) qw.eq(LaminationDaily::getStatus, status);
        qw.orderByDesc(LaminationDaily::getDate).orderByDesc(LaminationDaily::getId);

        Page<LaminationDaily> p = mapper.selectPage(new Page<>(page, size), qw);
        return R.ok(new PageResult<>(p.getTotal(), p.getRecords().stream().map(this::toVO).toList()));
    }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody LaminationDailyReq req) {
        LoginUser lu = CurrentUser.get();
        validateMachineType(req.getMachineType());
        checkUnique(req.getDate(), req.getOrderId(), null);
        LaminationDaily d = new LaminationDaily();
        applyReq(d, req);
        d.setUserId(lu.getId());
        d.setStatus(Boolean.TRUE.equals(req.getSubmit()) ? "PENDING" : "DRAFT");
        if (Boolean.TRUE.equals(req.getSubmit())) validateForSubmit(req);
        if (d.getSigner() == null || d.getSigner().isBlank()) d.setSigner(lu.getRealName());
        try {
            mapper.insert(d);
        } catch (DuplicateKeyException e) {
            // 并发创建唯一索引兜底：统一转业务码 2001
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
        LaminationDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireSee(d.getUserId());
        return R.ok(toVO(d));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @RequestBody LaminationDailyReq req) {
        LoginUser lu = CurrentUser.get();
        LaminationDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        if (!lu.getId().equals(d.getUserId()) && !lu.isAdmin()) throw BusinessException.of(403, "只能操作本人创建的记录");
        if (!"DRAFT".equals(d.getStatus()) && !"REJECTED".equals(d.getStatus())) {
            throw BusinessException.of(2002, "仅草稿或被退回的记录可以修改");
        }
        validateMachineType(req.getMachineType());
        applyReq(d, req);
        // 用合并后的实体做唯一性检查（req.date 可能为 null）
        checkUnique(d.getDate(), d.getOrderId(), id);
        if ("REJECTED".equals(d.getStatus())) d.setStatus("DRAFT");
        mapper.updateById(d);
        audit.log(MODULE, id, lu, "EDIT", "修改记录");
        return R.ok(toVO(d));
    }

    /** 员工仅能删本人草稿；主管仅能删本部门草稿/退回单；APPROVED 已锁定禁止删除 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        LoginUser lu = CurrentUser.get();
        LaminationDaily d = mapper.selectById(id);
        if (d == null) return R.ok();
        if ("WORKER".equals(lu.getRole())) {
            if (!lu.getId().equals(d.getUserId())) throw BusinessException.of(403, "只能操作本人创建的记录");
            if (!"DRAFT".equals(d.getStatus())) throw BusinessException.of(2002, "仅草稿可以删除");
        } else if (lu.isLead()) {
            requireLeadScope(d);
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
        LaminationDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        if (!lu.getId().equals(d.getUserId())) throw BusinessException.of(403, "只能操作本人创建的记录");
        LaminationDailyReq r = new LaminationDailyReq();
        r.setDate(d.getDate()); r.setOrderId(d.getOrderId()); r.setTimes(d.getTimes());
        r.setSpeed(d.getSpeed()); r.setMaterial(d.getMaterial()); r.setSpec(d.getSpec());
        r.setRequiredMeter(d.getRequiredMeter()); r.setActualMeter(d.getActualMeter());
        r.setMachineType(d.getMachineType()); r.setSigner(d.getSigner());
        validateForSubmit(r);
        // CAS 更新：仅 DRAFT → PENDING
        int rows = mapper.update(null, new LambdaUpdateWrapper<LaminationDaily>()
                .eq(LaminationDaily::getId, id)
                .eq(LaminationDaily::getStatus, "DRAFT")
                .set(LaminationDaily::getStatus, "PENDING"));
        if (rows == 0) throw BusinessException.of(2002, "当前状态不允许提交");
        audit.log(MODULE, id, lu, "SUBMIT", "提交审核");
        notifyLeaders(lu, d);
        return R.ok();
    }

    @PutMapping("/{id}/withdraw")
    public R<Void> withdraw(@PathVariable Long id) {
        LoginUser lu = CurrentUser.get();
        LaminationDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        if (!lu.getId().equals(d.getUserId())) throw BusinessException.of(403, "只能操作本人创建的记录");
        int rows = mapper.update(null, new LambdaUpdateWrapper<LaminationDaily>()
                .eq(LaminationDaily::getId, id)
                .eq(LaminationDaily::getStatus, "PENDING")
                .set(LaminationDaily::getStatus, "DRAFT"));
        if (rows == 0) throw BusinessException.of(2002, "当前状态不允许撤回");
        audit.log(MODULE, id, lu, "WITHDRAW", "撤回待审核记录");
        return R.ok();
    }

    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id) {
        LoginUser lu = CurrentUser.get();
        LaminationDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireLeadScope(d);
        int rows = mapper.update(null, new LambdaUpdateWrapper<LaminationDaily>()
                .eq(LaminationDaily::getId, id)
                .eq(LaminationDaily::getStatus, "PENDING")
                .set(LaminationDaily::getStatus, "APPROVED"));
        if (rows == 0) throw BusinessException.of(2002, "当前状态不允许审核");
        audit.log(MODULE, id, lu, "APPROVE", "审核通过");
        audit.notify(d.getUserId(), "APPROVED", "日报审核通过",
                "您提交的 " + d.getOrderId() + " 已审核通过并计入汇总", MODULE, id);
        return R.ok();
    }

    @PutMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        LoginUser lu = CurrentUser.get();
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) throw BusinessException.of(400, "退回必须填写原因");
        LaminationDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireLeadScope(d);
        int rows = mapper.update(null, new LambdaUpdateWrapper<LaminationDaily>()
                .eq(LaminationDaily::getId, id)
                .eq(LaminationDaily::getStatus, "PENDING")
                .set(LaminationDaily::getStatus, "REJECTED")
                .set(LaminationDaily::getRejectReason, reason));
        if (rows == 0) throw BusinessException.of(2002, "当前状态不允许退回");
        audit.log(MODULE, id, lu, "REJECT", "退回：" + reason);
        audit.notify(d.getUserId(), "REJECTED", "日报被退回",
                lu.getRealName() + " 退回了 " + d.getOrderId() + "：" + reason, MODULE, id);
        return R.ok();
    }

    @GetMapping("/{id}/timeline")
    public R<List<?>> timeline(@PathVariable Long id) {
        LaminationDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireSee(d.getUserId());
        return R.ok(audit.timeline(MODULE, id));
    }

    // ===== helpers =====

    private Map<String, Object> toVO(LaminationDaily d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("date", String.valueOf(d.getDate()));
        m.put("orderId", d.getOrderId());
        m.put("times", d.getTimes());
        m.put("speed", d.getSpeed());
        m.put("material", d.getMaterial());
        m.put("spec", d.getSpec());
        m.put("requiredMeter", d.getRequiredMeter());
        m.put("actualMeter", d.getActualMeter());
        m.put("rolls", d.getRolls());
        m.put("printRemain", d.getPrintRemain());
        m.put("machineType", d.getMachineType());
        m.put("adjustMeter", d.getAdjustMeter());
        m.put("signer", d.getSigner());
        m.put("status", d.getStatus());
        m.put("rejectReason", d.getRejectReason());
        m.put("createdAt", String.valueOf(d.getCreatedAt()));
        SysUser u = userMapper.selectById(d.getUserId());
        m.put("createdBy", u == null ? null : u.getRealName());
        return m;
    }

    private void applyReq(LaminationDaily d, LaminationDailyReq req) {
        if (req.getDate() != null) d.setDate(req.getDate());
        if (req.getOrderId() != null) d.setOrderId(req.getOrderId());
        if (req.getTimes() != null) d.setTimes(req.getTimes());
        if (req.getSpeed() != null) d.setSpeed(req.getSpeed());
        if (req.getMaterial() != null) d.setMaterial(req.getMaterial());
        if (req.getSpec() != null) d.setSpec(req.getSpec());
        if (req.getRequiredMeter() != null) d.setRequiredMeter(req.getRequiredMeter());
        if (req.getActualMeter() != null) d.setActualMeter(req.getActualMeter());
        if (req.getRolls() != null) d.setRolls(req.getRolls());
        if (req.getPrintRemain() != null) d.setPrintRemain(req.getPrintRemain());
        if (req.getMachineType() != null) d.setMachineType(req.getMachineType());
        if (req.getAdjustMeter() != null) d.setAdjustMeter(req.getAdjustMeter());
        if (req.getSigner() != null) d.setSigner(req.getSigner());
    }

    private void validateMachineType(String mt) {
        if (mt != null && !MACHINE_TYPES.contains(mt)) {
            throw BusinessException.of(400, "机型只能是 DRY 或 SOLVENT_FREE");
        }
    }

    private void validateForSubmit(LaminationDailyReq r) {
        if (r.getDate() == null || r.getOrderId() == null || r.getTimes() == null
                || r.getSpeed() == null || r.getMaterial() == null || r.getSpec() == null
                || r.getRequiredMeter() == null || r.getActualMeter() == null
                || r.getMachineType() == null || r.getSigner() == null) {
            throw BusinessException.of(2004, "存在未填写的必填字段，不能提交审核");
        }
    }

    private void checkUnique(java.time.LocalDate date, String orderId, Long excludeId) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<LaminationDaily>()
                .eq(LaminationDaily::getDate, date)
                .eq(LaminationDaily::getOrderId, orderId)
                .ne(excludeId != null, LaminationDaily::getId, excludeId));
        if (count > 0) throw BusinessException.of(2001, "该生产单号在当日已存在");
    }

    private void requireLeadScope(LaminationDaily d) {
        LoginUser lu = CurrentUser.get();
        SysUser creator = userMapper.selectById(d.getUserId());
        String dept = creator == null ? null : creator.getDept();
        if (!DailyRules.canSee(lu, d.getUserId(), dept)) {
            throw BusinessException.of(403, "无权审核该记录");
        }
    }

    /** 数据可见范围校验（API.md §6.1）：员工=本人，主管=本部门，管理=全部 */
    private void requireSee(Long creatorId) {
        LoginUser lu = CurrentUser.get();
        SysUser creator = userMapper.selectById(creatorId);
        String dept = creator == null ? null : creator.getDept();
        if (!DailyRules.canSee(lu, creatorId, dept)) {
            throw BusinessException.of(403, "无权查看该记录");
        }
    }

    private void notifyLeaders(LoginUser lu, LaminationDaily d) {
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
