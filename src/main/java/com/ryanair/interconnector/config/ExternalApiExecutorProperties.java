package com.ryanair.interconnector.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.api.executor")
public class ExternalApiExecutorProperties {

    /** Core pool size for the external API executor. */
    private int corePoolSize = 10;

    /** Maximum pool size for the external API executor. */
    private int maxPoolSize = 30;

    /** Queue capacity for the external API executor. */
    private int queueCapacity = 100;

}