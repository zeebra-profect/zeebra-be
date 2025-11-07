package com.zeebra;

import org.springframework.boot.SpringApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
//@SpringBootApplication
public class ZeebraApplicationTests {

    public static void main(String[] args) {
        SpringApplication.run(ZeebraApplication.class, args);
    }

}