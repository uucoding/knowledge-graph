package com.uka.knowledge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson序列化配置类
 * <p>
 * 解决Long类型ID返回前端时精度丢失问题
 * JavaScript的Number类型最大安全整数为2^53-1，超过会丢失精度
 * 将Long类型序列化为String可解决此问题
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置Jackson将Long类型序列化为String
     * 避免前端JavaScript处理大数值时精度丢失
     *
     * @return Jackson自定义配置
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // Long类型序列化为String
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
