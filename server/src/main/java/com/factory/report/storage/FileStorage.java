package com.factory.report.storage;

import org.springframework.web.multipart.MultipartFile;

/** 文件存储抽象：RustFS 对象存储（S3 兼容）为唯一实现（API.md §4 头像上传的后端实现） */
public interface FileStorage {

    /**
     * 保存上传文件到 avatar/ 目录
     * @return 可公开访问的 URL
     */
    String saveAvatar(MultipartFile file, String originalFilename) throws Exception;
}
