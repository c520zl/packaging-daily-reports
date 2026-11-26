package com.factory.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.factory.report.common.BusinessException;
import com.factory.report.entity.DailyAuditLog;
import com.factory.report.entity.SysNotice;
import com.factory.report.mapper.DailyAuditLogMapper;
import com.factory.report.mapper.SysNoticeMapper;
import com.factory.report.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 审计留痕 + 站内通知（API.md §6.5 timeline / §9） */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final DailyAuditLogMapper auditMapper;
    private final SysNoticeMapper noticeMapper;

    public void log(String module, Long recordId, LoginUser actor, String action, String detail) {
        DailyAuditLog log = new DailyAuditLog();
        log.setModule(module);
        log.setRecordId(recordId);
        log.setActorId(actor.getId());
        log.setActorName(actor.getRealName());
        log.setAction(action);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        auditMapper.insert(log);
    }

    /** 按时间正序返回某单据的完整时间线 */
    public List<DailyAuditLog> timeline(String module, Long recordId) {
        return auditMapper.selectList(new LambdaQueryWrapper<DailyAuditLog>()
                .eq(DailyAuditLog::getModule, module)
                .eq(DailyAuditLog::getRecordId, recordId)
                .orderByAsc(DailyAuditLog::getId));
    }

    /** 给指定用户发站内通知（content 超长截断，防 sys_notice.content VARCHAR(500) 溢出） */
    public void notify(Long userId, String type, String title, String content, String module, Long relatedId) {
        SysNotice n = new SysNotice();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content != null && content.length() > 500 ? content.substring(0, 500) : content);
        n.setRelatedModule(module);
        n.setRelatedId(relatedId);
        n.setReadFlag(false);
        n.setCreatedAt(LocalDateTime.now());
        noticeMapper.insert(n);
    }
}
