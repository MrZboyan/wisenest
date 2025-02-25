package com.demo.WiseNest.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedissonConfig {
    /**
     * 用途
     * 分布式锁：用于在分布式系统中实现资源的互斥访问。
     * 分布式集合：如分布式Map、Set、List等，用于在分布式环境中存储和操作数据。
     * 分布式队列：用于实现消息队列，支持发布/订阅模式等。
     * 分布式计数器：如原子计数器、分布式信号量等。
     * 分布式缓存：用于构建分布式缓存系统，提高系统的性能。
     */
    private String host;

    private Integer port;

    private Integer database;

    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setPassword(password)
                .setDnsMonitoringInterval(10000L);//  设置 DNS 监控间隔为 10 秒
        return Redisson.create(config);
    }
}



