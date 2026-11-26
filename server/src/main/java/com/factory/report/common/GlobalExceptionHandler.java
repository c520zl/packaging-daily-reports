package com.factory.report.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理：统一转成 R 结构 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数错误" : fe.getField() + ": " + fe.getDefaultMessage();
        return R.error(400, msg);
    }

    /** 请求体 JSON 解析失败（如日期格式错）返回 400 而不是 500 */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(org.springframework.http.converter.HttpMessageNotReadableException e) {
        return R.error(400, "请求体格式错误：" + e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleOther(Exception e) {
        log.error("unexpected error", e);
        return R.error(500, "服务器内部错误");
    }
}
