package com.zeebra.global.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "mainNotificationExecutor")
    public Executor mainNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(corePoolSize / 1);
        executor.setMaxPoolSize(corePoolSize);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("mainNoti-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setTaskDecorator(runnable -> {
            // 현재 스레드의 SecurityContext를 캡처
            SecurityContext context = SecurityContextHolder.getContext();
            return () -> {
                try {
                    // 비동기 스레드에 SecurityContext 설정
                    SecurityContextHolder.setContext(context);
                    runnable.run();
                } finally {
                    // 실행 후 정리
                    SecurityContextHolder.clearContext();
                }
            };
        });
        executor.initialize();
        return executor;
    }


    @Bean(name = "notificationWorkerExecutor")
    public Executor notificationWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(corePoolSize * 2);
        executor.setQueueCapacity(300);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("NotiWorker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setTaskDecorator(runnable -> {
            // 현재 스레드의 SecurityContext를 캡처
            SecurityContext context = SecurityContextHolder.getContext();
            return () -> {
                try {
                    // 비동기 스레드에 SecurityContext 설정
                    SecurityContextHolder.setContext(context);
                    runnable.run();
                } finally {
                    // 실행 후 정리
                    SecurityContextHolder.clearContext();
                }
            };
        });
        executor.initialize();
        return executor;
    }

    @Bean(name = "mainWebPushExecutor")
    public Executor mainWebPushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(corePoolSize / 1);
        executor.setMaxPoolSize(corePoolSize);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(10);
        executor.setThreadNamePrefix("mainWebPush-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }

    @Bean(name = "webPushWorkerExecutor")
    public Executor webPushWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(corePoolSize * 2);
        executor.setQueueCapacity(300);
        executor.setKeepAliveSeconds(10);
        executor.setThreadNamePrefix("webPushWorker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }

}
