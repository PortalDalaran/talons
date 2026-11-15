package com.example.talonstest;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import javax.sql.DataSource;

/**
 * 集成测试启动配置。
 *
 * <p>刻意放在 {@code com.example.talonstest} 包下，避免被
 * {@code TalonsConfiguration} 上的 {@code @ComponentScan("io.github.portaldalaran.talons")}
 * 二次扫描；自动装配完全依赖 jar 内的
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}，
 * 从而真实覆盖发布包里的自动配置路径。
 *
 * <p>数据源由本类显式声明：Spring Boot 3.5 在增量构建（{@code mvn test} 不带 clean）下，
 * 可能先评估 {@code MybatisPlusAutoConfiguration}，此时其
 * {@code @ConditionalOnSingleCandidate(DataSource)} 还看不到
 * {@code DataSourceAutoConfiguration} 注册的定义，于是跳过 {@code sqlSessionFactory}，
 * 所有 Mapper 报 "Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required"。
 * 用户配置类的 {@code @Bean} 先于 deferred 自动配置注册，可消除该顺序竞态；
 * MyBatis-Plus、SQL 初始化与 talons 自身的自动配置仍走发布包的真实路径。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@MapperScan("com.example.talonstest.fixture.mapper")
@ComponentScan("com.example.talonstest.fixture.service")
public class TestApplication {

    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        return DataSourceBuilder.create()
                .driverClassName(driverClassName)
                .url(url)
                .username(username)
                .password(password)
                .build();
    }
}
