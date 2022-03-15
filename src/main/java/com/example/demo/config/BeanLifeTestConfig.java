package com.example.demo.config;

import com.example.demo.model.pojo.BeanLife;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanLifeTestConfig {

    //@ConfigurationProperties(prefix = "library")  读取并与 bean 绑定。

    /**
     * 指定initMethod和destroyMethod 在BeanLife内的方法
     * @return
     */
//    @Bean(initMethod = "init", destroyMethod = "destroy")
    @Bean
    public BeanLife beanLife() {
        return new BeanLife();
    }
}