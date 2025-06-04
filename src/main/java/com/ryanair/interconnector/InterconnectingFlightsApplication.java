package com.ryanair.interconnector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableFeignClients
@EnableCaching
@EnableAsync
@SpringBootApplication
public class InterconnectingFlightsApplication {

  public static void main(String[] args) {
    SpringApplication.run(InterconnectingFlightsApplication.class, args);
  }

}
