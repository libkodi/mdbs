package com.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import io.github.libkodi.mdbs.interfaces.InitialDataSource;
import io.github.libkodi.mdbs.interfaces.InitialSqlSessionFactory;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MultiDataSourceConfiguration {
	/** 
     *  
     * 初始化数据源 
     * 
     * @description 为不同数据源设置连接参数 
     * @return InitialDataSource 
     */ 
    @Bean 
    public InitialDataSource initialDataSource() { 
        return (databaseId, pool, ctx) -> { 
        	pool.setDriver("com.p6spy.engine.spy.P6SpyDriver");
        	
        	if (databaseId.equals("primary")) {
        		pool.setUrl("jdbc:p6spy:sqlite:./db/test1.db");
        	} else if (databaseId.equals("second")) {
        		pool.setUrl("jdbc:p6spy:sqlite:./db/test2.db");
        	} else if (databaseId.equals("third")) {
        		pool.setUrl("jdbc:p6spy:sqlite:./db/test3.db");
        	}
        }; 
    } 
 
    private static Resource[] mappers = null; 
 
    /** 
     *  
     * 初始化mybatis设置 
     * 
     * @description 为不同的mybatis连接设置不同的参数和绑定mapper与添加工具之类的 
     * @return InitialSqlSessionFactory 
     */ 
    @Bean 
    public InitialSqlSessionFactory initialSqlSessionFactory() { 
        return (databaseId, factoryBean, ctx) -> { 
            try { 
                if (mappers == null) { 
                    mappers = new PathMatchingResourcePatternResolver().getResources("classpath*:/mapper/*.xml"); 
                }
                
                factoryBean.setMapperLocations(mappers); 
            } catch(Exception e) { 
                log.error("Failed to load mapper:", e.getMessage()); 
            } 
        }; 
    } 
}
