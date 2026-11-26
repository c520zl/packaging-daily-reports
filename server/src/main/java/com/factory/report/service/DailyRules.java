package com.factory.report.service;

import com.factory.report.common.BusinessException;
import com.factory.report.entity.PrintDaily;
import com.factory.report.mapper.PrintDailyMapper;
import com.factory.report.dto.PrintDailyReq;
import com.factory.report.security.CurrentUser;
import com.factory.report.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 三模块日报共用逻辑：可见范围、状态机（API.md §6.1）、校验。
 * 状态机在服务端强制，违反返回 2002。
 */
public final class DailyRules {

    public static final Set<String> STATUSES = Set.of("DRAFT", "PENDING", "APPROVED", "REJECTED");

    private DailyRules() {}

    /** 数据可见范围：员工=本人；主管=本部门（按创建人 dept 过滤需联表，这里按创建人判断）；管理=全部 */
    public static boolean canSee(LoginUser lu, Long creatorId, String creatorDept) {
        if (lu.isAdmin() || lu.isManagement()) return true;
        if (lu.isLead()) return lu.getDept() != null && lu.getDept().equals(creatorDept);
        return lu.getId().equals(creatorId);
    }

    /** 校验当前状态允许的操作 */
    public static void requireStatus(String current, String expected, String action) {
        if (!expected.equals(current)) {
            throw BusinessException.of(2002, "当前状态不允许" + action);
        }
    }

    /** 提交前强校验（API.md 2004）：关键必填字段缺失不允许进审核 */
    public static void validatePrintForSubmit(PrintDailyReq r) {
        if (r.getDate() == null || isBlank(r.getOrderId()) || r.getOnboardTime() == null
                || r.getSpeed() == null || isBlank(r.getMaterial()) || isBlank(r.getSpec())
                || r.getRequiredMeter() == null || r.getActualMeter() == null
                || r.getColors() == null || r.getColors().isEmpty() || isBlank(r.getSigner())) {
            throw BusinessException.of(2004, "存在未填写的必填字段，不能提交审核");
        }
    }

    public static void validateBagEndMeter(java.math.BigDecimal incoming, java.math.BigDecimal end) {
        if (incoming != null && end != null && end.compareTo(incoming) > 0) {
            throw BusinessException.of(2003, "结束米数不能大于来料米数");
        }
    }

    public static String joinColors(List<String> colors) {
        return colors == null ? null : String.join(",", colors);
    }

    public static java.util.List<String> splitColors(String joined) {
        return joined == null || joined.isBlank() ? List.of() : Arrays.asList(joined.split(","));
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
