package com.goorm.tablepick.domain.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);      // 기본 실행 대기 스레드 수
        executor.setMaxPoolSize(3);      // 최대 스레드 수
        executor.setQueueCapacity(500);    // 최대 큐 크기
        executor.setThreadNamePrefix("mail-async-");
        executor.initialize();
        return executor;
    }
}