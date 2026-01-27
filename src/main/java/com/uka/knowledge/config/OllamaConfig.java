package com.uka.knowledge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Ollama配置类
 * <p>
 * 配置Ollama API调用的超时时间等参数
 * 大模型推理耗时较长，需要设置足够的超时时间
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;


    /**
     * 配置自定义的OllamaApi
     * 设置较长的超时时间以适应大模型推理
     *
     * @return OllamaApi
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        // 创建自定义的请求工厂，设置超时时间
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时：30秒
        factory.setConnectTimeout(Duration.ofSeconds(30));
        // 读取超时：5分钟（大模型推理可能较慢）
        factory.setReadTimeout(Duration.ofMinutes(5));

        // 创建带自定义超时的RestClient
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory);
    }
}
