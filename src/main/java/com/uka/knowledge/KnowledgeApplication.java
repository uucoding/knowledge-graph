package com.uka.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jReactiveRepositoriesAutoConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 知识图谱系统启动类
 * <p>
 * 本系统是一个基于Spring Boot的知识图谱管理平台，集成了：
 * - 多格式文档解析（PDF、Word、TXT、Markdown）
 * - 图片OCR识别
 * - 知识图谱存储与查询（Neo4j）
 * - 向量数据库存储（Milvus）
 * - 大模型语义分析（Ollama）
 * </p>
 *
 * @author uka
 * @version 1.0
 * @since 2024
 */
@Slf4j
@SpringBootApplication(exclude = {
        // 排除Neo4j响应式自动配置，避免事务管理器冲突
        Neo4jReactiveDataAutoConfiguration.class,
        Neo4jReactiveRepositoriesAutoConfiguration.class
})
@MapperScan("com.uka.knowledge.mapper")
@EnableNeo4jRepositories("com.uka.knowledge.repository")
@EnableAsync
public class KnowledgeApplication {

    /**
     * 应用程序入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeApplication.class, args);
        System.out.println("============================================");
        System.out.println("      知识图谱系统启动成功！");
        System.out.println("      API文档地址: http://localhost:8080/doc.html");
        System.out.println("============================================");
    }
}
