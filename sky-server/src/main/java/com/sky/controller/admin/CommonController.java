package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Slf4j
@Api(tags = "通用接口")
public class CommonController {
    @Autowired
    private AliOssUtil aliOSSUtil;

    /**
     * 文件上传
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation(("文件上传"))
    public Result<String> uploadFile(MultipartFile file) {
        try {
            //原始文件名
            String originalFilename = file.getOriginalFilename();
            log.info("上传文件:{}", originalFilename);
            //截取原始文件名的后缀   picture.png
            String suffix = null;
            if (originalFilename != null) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            //构造新文件名称
            String objectName = UUID.randomUUID() + suffix;
            //文件的请求路径
            String url = aliOSSUtil.upload(file.getBytes(), objectName);
            return Result.success(url);
        } catch (Exception e) {
            return Result.error("上传文件失败:" + e.getMessage());
        }
    }
}
