package com.hidoc.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class VirtualThreadConfig implements AsyncConfigurer {

    @Bean
    public TaskExecutor taskExecutor() {
        // Wrap virtual thread executor for Spring TaskExecutor compatibility
        return new ConcurrentTaskExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Override
    public Executor getAsyncExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
