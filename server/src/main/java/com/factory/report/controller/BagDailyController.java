package com.factory.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.factory.report.common.BusinessException;
import com.factory.report.common.PageResult;
import com.factory.report.common.R;
import com.factory.report.dto.BagDailyReq;
import com.factory.report.entity.BagDaily;
import com.factory.report.entity.SysUser;
import com.factory.report.mapper.BagDailyMapper;
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

/** 制袋部日报，见 API.md §6（module=BAGMAKING）；结束米数 ≤ 来料米数（2003） */
@RestController
@RequestMapping("/api/bag-daily")
@RequiredArgsConstructor
public class BagDailyController {

    public static final String MODULE = "BAGMAKING";
    private final BagDailyMapper mapper;
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
            @RequestParam(required = false) String machine,
            @RequestParam(required = false) String shift,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") boolean mine) {
        LoginUser lu = CurrentUser.get();
        LambdaQueryWrapper<BagDaily> qw = new LambdaQueryWrapper<>();
        if ("WORKER".equals(lu.getRole()) || mine) qw.eq(BagDaily::getUserId, lu.getId());
        if (date != null && !date.isBlank()) qw.eq(BagDaily::getDate, date);
        if (startDate != null && !startDate.isBlank()) qw.ge(BagDaily::getDate, startDate);
        if (endDate != null && !endDate.isBlank()) qw.le(BagDaily::getDate, endDate);
        if (orderId != null && !orderId.isBlank()) qw.like(BagDaily::getOrderId, orderId);
        if (machine != null && !machine.isBlank()) qw.eq(BagDaily::getMachine, machine);
        if (shift != null && !shift.isBlank()) qw.eq(BagDaily::getShift, shift);
        if (status != null && !status.isBlank()) qw.eq(BagDaily::getStatus, status);
        qw.orderByDesc(BagDaily::getDate).orderByDesc(BagDaily::getId);

        Page<BagDaily> p = mapper.selectPage(new Page<>(page, size), qw);
        return R.ok(new PageResult<>(p.getTotal(), p.getRecords().stream().map(this::toVO).toList()));
    }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody BagDailyReq req) {
        LoginUser lu = CurrentUser.get();
        DailyRules.validateBagEndMeter(req.getIncomingMeter(), req.getEndMeter());
        checkUnique(req.getDate(), req.getMachine(), req.getShift(), req.getOrderId(), null);
        BagDaily d = new BagDaily();
        applyReq(d, req);
        d.setUserId(lu.getId());
        d.setStatus(Boolean.TRUE.equals(req.getSubmit()) ? "PENDING" : "DRAFT");
        if (Boolean.TRUE.equals(req.getSubmit())) validateForSubmit(req);
        if (d.getSigner() == null || d.getSigner().isBlank()) d.setSigner(lu.getRealName());
        try {
            mapper.insert(d);
        } catch (DuplicateKeyException e) {
            // 并发创建唯一索引兜底：统一转业务码 2001
            throw BusinessException.of(2001, "该机台班次下此生产单号当日已存在");
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
        BagDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireSee(d.getUserId());
        return R.ok(toVO(d));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @RequestBody BagDailyReq req) {
        LoginUser lu = CurrentUser.get();
        BagDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        if (!lu.getId().equals(d.getUserId()) && !lu.isAdmin()) throw BusinessException.of(403, "只能操作本人创建的记录");
        if (!"DRAFT".equals(d.getStatus()) && !"REJECTED".equals(d.getStatus())) {
            throw BusinessException.of(2002, "仅草稿或被退回的记录可以修改");
        }
        applyReq(d, req);
        // 用合并后的实体校验（部分更新时 req 单边为 null 会绕过 2003 校验）
        DailyRules.validateBagEndMeter(d.getIncomingMeter(), d.getEndMeter());
        checkUnique(d.getDate(), d.getMachine(), d.getShift(), d.getOrderId(), id);
        if ("REJECTED".equals(d.getStatus())) d.setStatus("DRAFT");
        mapper.updateById(d);
        audit.log(MODULE, id, lu, "EDIT", "修改记录");
        return R.ok(toVO(d));
    }

    /** 员工仅能删本人草稿；主管仅能删本部门草稿/退回单；APPROVED 已锁定禁止删除 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        LoginUser lu = CurrentUser.get();
        BagDaily d = mapper.selectById(id);
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
        BagDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        if (!lu.getId().equals(d.getUserId())) throw BusinessException.of(403, "只能操作本人创建的记录");
        // 提交前用实体做 2003 校验（草稿可能由部分更新产生绕过校验的数据）
        DailyRules.validateBagEndMeter(d.getIncomingMeter(), d.getEndMeter());
        BagDailyReq r = new BagDailyReq();
        r.setDate(d.getDate()); r.setShift(d.getShift()); r.setMachine(d.getMachine());
        r.setOrderId(d.getOrderId()); r.setBagType(d.getBagType()); r.setSpec(d.getSpec());
        r.setIncomingMeter(d.getIncomingMeter()); r.setEndMeter(d.getEndMeter());
        r.setCompletedQty(d.getCompletedQty()); r.setWorkStart(d.getWorkStart());
        r.setWorkEnd(d.getWorkEnd()); r.setSigner(d.getSigner());
        validateForSubmit(r);
        // CAS 更新：仅 DRAFT → PENDING
        int rows = mapper.update(null, new LambdaUpdateWrapper<BagDaily>()
                .eq(BagDaily::getId, id)
                .eq(BagDaily::getStatus, "DRAFT")
                .set(BagDaily::getStatus, "PENDING"));
        if (rows == 0) throw BusinessException.of(2002, "当前状态不允许提交");
        audit.log(MODULE, id, lu, "SUBMIT", "提交审核");
        notifyLeaders(lu, d);
        return R.ok();
    }

    @PutMapping("/{id}/withdraw")
    public R<Void> withdraw(@PathVariable Long id) {
        LoginUser lu = CurrentUser.get();
        BagDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        if (!lu.getId().equals(d.getUserId())) throw BusinessException.of(403, "只能操作本人创建的记录");
        int rows = mapper.update(null, new LambdaUpdateWrapper<BagDaily>()
                .eq(BagDaily::getId, id)
                .eq(BagDaily::getStatus, "PENDING")
                .set(BagDaily::getStatus, "DRAFT"));
        if (rows == 0) throw BusinessException.of(2002, "当前状态不允许撤回");
        audit.log(MODULE, id, lu, "WITHDRAW", "撤回待审核记录");
        return R.ok();
    }

    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id) {
        LoginUser lu = CurrentUser.get();
        BagDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireLeadScope(d);
        int rows = mapper.update(null, new LambdaUpdateWrapper<BagDaily>()
                .eq(BagDaily::getId, id)
                .eq(BagDaily::getStatus, "PENDING")
                .set(BagDaily::getStatus, "APPROVED"));
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
        BagDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireLeadScope(d);
        int rows = mapper.update(null, new LambdaUpdateWrapper<BagDaily>()
                .eq(BagDaily::getId, id)
                .eq(BagDaily::getStatus, "PENDING")
                .set(BagDaily::getStatus, "REJECTED")
                .set(BagDaily::getRejectReason, reason));
        if (rows == 0) throw BusinessException.of(2002, "当前状态不允许退回");
        audit.log(MODULE, id, lu, "REJECT", "退回：" + reason);
        audit.notify(d.getUserId(), "REJECTED", "日报被退回",
                lu.getRealName() + " 退回了 " + d.getOrderId() + "：" + reason, MODULE, id);
        return R.ok();
    }

    @GetMapping("/{id}/timeline")
    public R<List<?>> timeline(@PathVariable Long id) {
        BagDaily d = mapper.selectById(id);
        if (d == null) throw BusinessException.of(404, "记录不存在");
        requireSee(d.getUserId());
        return R.ok(audit.timeline(MODULE, id));
    }

    // ===== helpers =====

    private Map<String, Object> toVO(BagDaily d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("date", String.valueOf(d.getDate()));
        m.put("shift", d.getShift());
        m.put("machine", d.getMachine());
        m.put("picker", d.getPicker());
        m.put("leader", d.getLeader());
        m.put("speed", d.getSpeed());
        m.put("orderId", d.getOrderId());
        m.put("bagType", d.getBagType());
        m.put("spec", d.getSpec());
        m.put("incomingMeter", d.getIncomingMeter());
        m.put("adjustMeter", d.getAdjustMeter());
        m.put("endMeter", d.getEndMeter());
        m.put("completedQty", d.getCompletedQty());
        m.put("wasteKg", d.getWasteKg());
        m.put("remainKg", d.getRemainKg());
        m.put("workStart", d.getWorkStart() == null ? null : String.valueOf(d.getWorkStart()));
        m.put("workEnd", d.getWorkEnd() == null ? null : String.valueOf(d.getWorkEnd()));
        m.put("abnormalReason", d.getAbnormalReason());
        m.put("defectQty", d.getDefectQty());
        m.put("signer", d.getSigner());
        m.put("status", d.getStatus());
        m.put("rejectReason", d.getRejectReason());
        m.put("createdAt", String.valueOf(d.getCreatedAt()));
        SysUser u = userMapper.selectById(d.getUserId());
        m.put("createdBy", u == null ? null : u.getRealName());
        return m;
    }

    private void applyReq(BagDaily d, BagDailyReq req) {
        if (req.getDate() != null) d.setDate(req.getDate());
        if (req.getShift() != null) d.setShift(req.getShift());
        if (req.getMachine() != null) d.setMachine(req.getMachine());
        if (req.getPicker() != null) d.setPicker(req.getPicker());
        if (req.getLeader() != null) d.setLeader(req.getLeader());
        if (req.getSpeed() != null) d.setSpeed(req.getSpeed());
        if (req.getOrderId() != null) d.setOrderId(req.getOrderId());
        if (req.getBagType() != null) d.setBagType(req.getBagType());
        if (req.getSpec() != null) d.setSpec(req.getSpec());
        if (req.getIncomingMeter() != null) d.setIncomingMeter(req.getIncomingMeter());
        if (req.getAdjustMeter() != null) d.setAdjustMeter(req.getAdjustMeter());
        if (req.getEndMeter() != null) d.setEndMeter(req.getEndMeter());
        if (req.getCompletedQty() != null) d.setCompletedQty(req.getCompletedQty());
        if (req.getWasteKg() != null) d.setWasteKg(req.getWasteKg());
        if (req.getRemainKg() != null) d.setRemainKg(req.getRemainKg());
        if (req.getWorkStart() != null) d.setWorkStart(req.getWorkStart());
        if (req.getWorkEnd() != null) d.setWorkEnd(req.getWorkEnd());
        if (req.getAbnormalReason() != null) d.setAbnormalReason(req.getAbnormalReason());
        if (req.getDefectQty() != null) d.setDefectQty(req.getDefectQty());
        if (req.getSigner() != null) d.setSigner(req.getSigner());
    }

    private void validateForSubmit(BagDailyReq r) {
        if (r.getDate() == null || r.getShift() == null || r.getMachine() == null
                || r.getOrderId() == null || r.getBagType() == null || r.getSpec() == null
                || r.getIncomingMeter() == null || r.getEndMeter() == null
                || r.getCompletedQty() == null || r.getWorkStart() == null
                || r.getWorkEnd() == null || r.getSigner() == null) {
            throw BusinessException.of(2004, "存在未填写的必填字段，不能提交审核");
        }
    }

    private void checkUnique(java.time.LocalDate date, String machine, String shift, String orderId, Long excludeId) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<BagDaily>()
                .eq(BagDaily::getDate, date)
                .eq(BagDaily::getMachine, machine)
                .eq(BagDaily::getShift, shift)
                .eq(BagDaily::getOrderId, orderId)
                .ne(excludeId != null, BagDaily::getId, excludeId));
        if (count > 0) throw BusinessException.of(2001, "该机台班次下此生产单号当日已存在");
    }

    private void requireLeadScope(BagDaily d) {
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

    private void notifyLeaders(LoginUser lu, BagDaily d) {
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
