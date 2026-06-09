package com.ecommerce.payment.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${db.auth.url}")
    private String authDbUrl;

    @Value("${db.inventory.url}")
    private String inventoryDbUrl;

    @Bean(name = "authDataSource")
    public DataSource authDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(authDbUrl);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        return dataSource;
    }

    @Bean(name = "inventoryDataSource")
    public DataSource inventoryDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(inventoryDbUrl);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        return dataSource;
    }
}
