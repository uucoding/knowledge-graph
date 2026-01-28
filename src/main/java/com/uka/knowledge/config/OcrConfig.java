package com.uka.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OCR配置类
 * <p>
 * 配置Tesseract OCR引擎的相关参数
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ocr")
public class OcrConfig {

    /**
     * Tesseract 库文件地址
     */
    private String libraryFile = "/opt/homebrew/lib";

    /**
     * Tesseract数据目录路径
     * 需要包含语言训练数据文件（如chi_sim.traineddata）
     */
    private String dataPath = "/opt/homebrew/share/tessdata";

    /**
     * OCR识别语言
     * chi_sim: 简体中文
     * eng: 英文
     * 可组合使用，如：chi_sim+eng
     */
    private String language = "chi_sim+eng";
}
