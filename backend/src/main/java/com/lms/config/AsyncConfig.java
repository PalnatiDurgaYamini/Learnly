package com.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Thread pool that runs background jobs away from the HTTP request thread. */
@Configuration @EnableAsync
public class AsyncConfig {
    @Bean("taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(3);
        ex.setMaxPoolSize(5);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("LMS-Async-");
        ex.initialize();
        return ex;
    }
}
