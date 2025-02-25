package com.demo.WiseNest.controller;

import cn.hutool.core.io.FileUtil;
import com.demo.WiseNest.common.BaseResponse;
import com.demo.WiseNest.common.ResultUtils;
import com.demo.WiseNest.exception.BusinessException;
import com.demo.WiseNest.exception.ErrorCode;
import com.demo.WiseNest.utils.AliOSSUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/aliUpload")
public class UploadController {

    @Resource
    private AliOSSUtils aliOSSUtils;

    @PostMapping
    public BaseResponse<String> upload(MultipartFile image) throws Exception {

        // 校验文件
        validFile(image);

        // 调用阿里云OSS工具类，将上传上来的文件存入阿里云
        String url = aliOSSUtils.upload(image);

        // 将图片上传完成后的url返回，用于浏览器回显展示
        return ResultUtils.success(url);
    }

    /**
     * 校验文件
     *
     * @param image 图片
     */
    private void validFile(MultipartFile image) {
        // 文件大小
        long fileSize = image.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(image.getOriginalFilename());
        final long ONE_M = 2 * 1024 * 1024L; // 2M
        if (fileSize > ONE_M) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
        }
        if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
        }
    }
}
