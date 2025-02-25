package com.demo.WiseNest.config;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@Data
public class VipSchedulerConfig {

    @Bean
    public Scheduler vipScheduler() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(@NotNull Runnable runnable) {
                Thread t = new Thread(runnable, "VIPThreadPool-" + threadNumber.getAndIncrement());
                t.setDaemon(false); // 设置为非守护线程
                return t;
            }
        };
        // 自定义线程池
        ThreadPoolExecutor executorService = new ThreadPoolExecutor(
                10, // 核心线程数
                20, // 最大线程数
                60L, // 空闲线程存活时间
                TimeUnit.SECONDS, // 时间单位
                new LinkedBlockingQueue<>(100), // 阻塞队列
                threadFactory, // 自定义线程工厂
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
        return Schedulers.from(executorService);
    }
}

