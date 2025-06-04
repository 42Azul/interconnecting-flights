package com.ryanair.interconnector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

/**
 * Example configuration class for setting up an asynchronous executor.
 * This configuration can be improved in the future by setting properties in the application.properties file instead of hardcoding them.
 */

@Configuration
public class AsyncConfig {

  @Bean(name = "externalApiExecutor")
  public Executor externalApiExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(30);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("externalApi-");
    executor.initialize();
    return executor;
  }
}
