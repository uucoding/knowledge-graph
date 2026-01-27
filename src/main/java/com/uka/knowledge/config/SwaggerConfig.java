package com.uka.knowledge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/Knife4j接口文档配置类
 * <p>
 * 配置API文档的基本信息
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置OpenAPI文档信息
     *
     * @return OpenAPI配置对象
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("知识图谱系统 API")
                        .version("1.0.0")
                        .description("基于Spring Boot的知识图谱管理系统，支持文档解析、OCR识别、知识图谱构建与查询")
                        .contact(new Contact()
                                .name("uka")
                                .email("uka@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
