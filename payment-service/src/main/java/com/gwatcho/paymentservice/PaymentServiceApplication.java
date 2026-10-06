package com.gwatcho.paymentservice;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@Slf4j
@SpringBootApplication
public class PaymentServiceApplication {

    @Bean
    CommandLineRunner verifyDatasource(DataSource dataSource) {
        return args -> {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            log.info("========================================");
            log.info("DATASOURCE VERIFICATION");
            log.info("JDBC URL: {}", hikari.getJdbcUrl());
            log.info("USERNAME: {}", hikari.getUsername());
            log.info("========================================");
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}
