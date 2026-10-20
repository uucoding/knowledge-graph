package com.uka.knowledge.controller;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 骨架探活接口
 * <p>
 * 用于验证 PostgreSQL(pgvector) 与 Redis 的装配与连通性
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    private final StringRedisTemplate redisTemplate;

    public HealthController(DataSource dataSource, StringRedisTemplate redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("application", "knowledge-graph");
        result.put("time", OffsetDateTime.now());
        result.put("database", databaseStatus());
        result.put("redis", redisStatus());
        return result;
    }

    private String databaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN: " + e.getMessage();
        }
    }

    private String redisStatus() {
        try {
            String pong = redisTemplate.execute((RedisCallback<String>) RedisConnection::ping);
            return "UP: " + pong;
        } catch (Exception e) {
            return "DOWN: " + e.getMessage();
        }
    }
}
