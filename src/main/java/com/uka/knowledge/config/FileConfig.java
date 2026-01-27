package com.uka.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 文件存储配置类
 * <p>
 * 配置文件上传路径和允许的文件类型
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "file")
public class FileConfig {

    /**
     * 文件上传目录
     */
    private String uploadPath = "./uploads";

    /**
     * 允许上传的文件类型（逗号分隔）
     */
    private String allowedTypes = "pdf,doc,docx,txt,md,png,jpg,jpeg,gif,bmp";

    /**
     * 获取允许的文件类型列表
     *
     * @return 文件类型列表
     */
    public List<String> getAllowedTypeList() {
        return Arrays.asList(allowedTypes.toLowerCase().split(","));
    }

    /**
     * 检查文件类型是否允许
     *
     * @param fileExtension 文件扩展名
     * @return 是否允许
     */
    public boolean isAllowedType(String fileExtension) {
        if (fileExtension == null || fileExtension.isEmpty()) {
            return false;
        }
        return getAllowedTypeList().contains(fileExtension.toLowerCase());
    }
}
