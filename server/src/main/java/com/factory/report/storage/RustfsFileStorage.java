package com.factory.report.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * RustFS 对象存储（S3 兼容协议，系统唯一文件存储实现）
 * 通过 MinIO Java SDK 走标准 S3 接口直连，bucket 自动创建 + 公开读策略（头像 URL 直接可访问）
 * 注意：构造器不连存储（懒初始化）——RustFS 未就绪/网络抖动时应用仍可启动，仅头像上传不可用
 */
@Component
public class RustfsFileStorage implements FileStorage {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(RustfsFileStorage.class);

    private final MinioClient client;
    private final String bucket;
    private final String publicUrl;
    private volatile boolean bucketReady = false;

    public RustfsFileStorage(@Value("${storage.rustfs.endpoint}") String endpoint,
                             @Value("${storage.rustfs.access-key}") String accessKey,
                             @Value("${storage.rustfs.secret-key}") String secretKey,
                             @Value("${storage.rustfs.bucket}") String bucket,
                             @Value("${storage.rustfs.public-url}") String publicUrl) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
        this.publicUrl = publicUrl;
    }

    /** 懒初始化：首次上传时确保 bucket 存在；失败抛业务异常但不影响应用其他功能 */
    private synchronized void ensureBucket() throws Exception {
        if (bucketReady) return;
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        // 公开读：匿名 GET 可直接下载（与 mc anonymous set download 等效，双保险）
        String policy = """
                {"Version":"2012-10-17","Statement":[
                  {"Effect":"Allow","Principal":{"AWS":["*"]},
                   "Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}
                ]}""".formatted(bucket);
        client.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
        bucketReady = true;
        log.info("RustFS bucket '{}' 就绪", bucket);
    }

    @Override
    public String saveAvatar(MultipartFile file, String originalFilename) throws Exception {
        ensureBucket();
        String name = "avatar/" + UUID.randomUUID() + suffix(originalFilename);
        try (var in = new ByteArrayInputStream(file.getBytes())) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(name)
                    .stream(in, file.getSize(), -1)
                    .contentType(contentType(originalFilename))
                    .build());
        }
        return publicUrl + "/" + bucket + "/" + name;
    }

    private String contentType(String name) {
        String n = name == null ? "" : name.toLowerCase();
        return n.endsWith(".png") ? "image/png" : "image/jpeg";
    }

    private String suffix(String name) {
        String n = name == null ? "" : name.toLowerCase();
        return n.endsWith(".png") ? ".png" : ".jpg";
    }
}
