package com.demo.WiseNest.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.demo.WiseNest.model.entity.AliOSSProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Component
public class AliOSSUtils {

    @Resource
    private AliOSSProperties aliOSSProperties;

    /**
     * 实现上传图片到OSS
     */
    public String upload(MultipartFile multipartFile) throws Exception {
        // 获取上传的文件的输入流
        InputStream inputStream = multipartFile.getInputStream();
        // 避免文件覆盖
        String originalFilename = multipartFile.getOriginalFilename();
        String fileName = null;
        if (originalFilename != null) {
            fileName = UUID.randomUUID() + originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // 上传文件到 OSS
        OSS ossClient = new OSSClientBuilder().build(aliOSSProperties.getEndpoint(),
                aliOSSProperties.getAccessKeyId(), aliOSSProperties.getAccessKeySecret());
        ossClient.putObject(aliOSSProperties.getBucketName(), fileName, inputStream);
        // 文件访问路径
        String url = aliOSSProperties.getEndpoint().split("//")[0] + "//"
                + aliOSSProperties.getBucketName() + "."
                + aliOSSProperties.getEndpoint().split("//")[1]
                + "/" + fileName;
        // 关闭ossClient
        ossClient.shutdown();
        // 返回访问路径
        return url;
    }
}
