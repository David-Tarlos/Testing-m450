package ch.tbz.yahoot.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

@Configuration
class SchedulerConfig {

    @Bean
    SimpleAsyncTaskScheduler roundScheduler() {
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setThreadNamePrefix("round-");
        scheduler.setVirtualThreads(true);
        return scheduler;
    }
}
