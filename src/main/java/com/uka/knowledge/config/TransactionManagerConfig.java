package com.uka.knowledge.config;

import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * 事务管理器配置类
 * <p>
 * 解决MyBatis-Plus和Neo4j事务管理器冲突问题
 * 配置MySQL事务管理器为主事务管理器
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Configuration
@EnableTransactionManagement
public class TransactionManagerConfig {

    /**
     * 配置MySQL/JDBC事务管理器为主事务管理器
     * 使用@Primary注解标记为默认事务管理器
     *
     * @param dataSource 数据源
     * @return 事务管理器
     */
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * Neo4j事务管理器
     * 在需要Neo4j事务时，使用 @Transactional("neo4jTransactionManager") 指定
     *
     * @param driver Neo4j驱动
     * @return Neo4j事务管理器
     */
    @Bean(name = "neo4jTransactionManager")
    public Neo4jTransactionManager neo4jTransactionManager(Driver driver) {
        return new Neo4jTransactionManager(driver);
    }
}
