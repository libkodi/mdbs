package com.github.libkodi.mdbs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.libkodi.mdbs.properties.MultipartDataSourceProperties;

@Configuration
@EnableConfigurationProperties(MultipartDataSourceProperties.class)
public class MultipartDataSourceConfiguration {
	/**
	 * 配置信息
	 */
	@Autowired
	private MultipartDataSourceProperties properties;
	
	/**
	 * 
	 * 创建一个bean供Service注入
	 */
	@Bean
	@ConditionalOnMissingBean(MultipartDataSource.class)
	public MultipartDataSource getMultipartDataSource() {
		return MultipartDataSource.getInstance(properties);
	}
}
