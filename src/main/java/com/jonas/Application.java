package com.jonas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

/**
 * 【 enter the class description 】
 *
 * @author shenjy 2019/06/10
 */
@SpringBootApplication
//@EnableIntegration 是开启 Spring Integration 功能的核心注解。它会激活 Spring Integration 的各种组件，包括对 @MessagingGateway 的处理。
@EnableIntegration
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
