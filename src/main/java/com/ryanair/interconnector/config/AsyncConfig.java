package com.ryanair.interconnector.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.Executor;

/**
 * Configuration class for setting up an asynchronous executor using Spring Configuration Properties
 */

@Configuration
@EnableConfigurationProperties(ExternalApiExecutorProperties.class)
public class AsyncConfig {

  @Bean(name = "externalApiExecutor")
  public Executor externalApiExecutor(ExternalApiExecutorProperties properties) {
    return new ThreadPoolTaskExecutorBuilder()
        .corePoolSize(properties.getCorePoolSize())
        .maxPoolSize(properties.getMaxPoolSize())
        .queueCapacity(properties.getQueueCapacity())
        .threadNamePrefix("externalApi-")
        .build();
  }
}
