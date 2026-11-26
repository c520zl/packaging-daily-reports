package com.factory.report.common;

import java.util.List;

/** 分页响应结构，见 API.md §1 */
public record PageResult<T>(long total, List<T> records) {
}
