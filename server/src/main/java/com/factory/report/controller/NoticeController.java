package com.factory.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.factory.report.common.PageResult;
import com.factory.report.common.R;
import com.factory.report.entity.SysNotice;
import com.factory.report.mapper.SysNoticeMapper;
import com.factory.report.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 站内通知，见 API.md §9 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final SysNoticeMapper mapper;

    @GetMapping
    public R<PageResult<SysNotice>> list(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "20") long size,
                                         @RequestParam(required = false) Boolean unread) {
        LambdaQueryWrapper<SysNotice> qw = new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getUserId, CurrentUser.get().getId())
                .eq(unread != null && unread, SysNotice::getReadFlag, false)
                .orderByDesc(SysNotice::getId);
        Page<SysNotice> p = mapper.selectPage(new Page<>(page, size), qw);
        return R.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @PutMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        SysNotice n = mapper.selectById(id);
        if (n != null && n.getUserId().equals(CurrentUser.get().getId())) {
            n.setReadFlag(true);
            mapper.updateById(n);
        }
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> readAll() {
        SysNotice patch = new SysNotice();
        patch.setReadFlag(true);
        mapper.update(patch, new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getUserId, CurrentUser.get().getId())
                .eq(SysNotice::getReadFlag, false));
        return R.ok();
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(mapper.selectCount(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getUserId, CurrentUser.get().getId())
                .eq(SysNotice::getReadFlag, false)));
    }
}
