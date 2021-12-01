package com.test.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import io.github.libkodi.mdbs.MultiDataSource;
import lombok.extern.slf4j.Slf4j; 
 
/**
 * 手动配置主数据源
 * 注意1: 数据库的连接信息不能有错
 * 注意2: mapper中的语句与返回类型不能有错
 */
@Configuration 
@Slf4j
public class JdbcConfiguration { 
    @Autowired 
    private MultiDataSource mdbs; 
    
    @Bean 
    @Primary 
    public DataSource getDataSource() { 
        return mdbs.getDataSource("primary"); 
    } 
 
    @Bean 
    @Primary 
    public SqlSessionFactory getSqlSessionFactory() { 
        try { 
            return mdbs.getSqlSessionFactory("primary"); 
        } catch (Exception e) { 
            log.error("Failed to load sqlsessionfactory", e.getMessage()); 
            return null; 
        } 
    } 
 
    @Primary 
    @Bean 
    public DataSourceTransactionManager getTransactionManager(DataSource ds) { 
        return new DataSourceTransactionManager(ds); 
    } 
 
    @Primary 
    @Bean 
    public SqlSessionTemplate getSqlSessionTemplate(SqlSessionFactory factory) { 
    	return new SqlSessionTemplate(factory); 
    } 
}
