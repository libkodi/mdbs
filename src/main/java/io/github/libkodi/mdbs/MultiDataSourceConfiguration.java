package io.github.libkodi.mdbs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.libkodi.mdbs.properties.MultiDataSourceProperties;

@Configuration
@EnableConfigurationProperties(MultiDataSourceProperties.class)
public class MultiDataSourceConfiguration {
	/**
	 * 配置信息
	 */
	@Autowired
	private MultiDataSourceProperties properties;
	
	/**
	 * 
	 * 创建一个bean供Service注入
	 */
	@Bean
	@ConditionalOnMissingBean(MultiDataSource.class)
	public MultiDataSource getMultipartDataSource() {
		return MultiDataSource.getInstance(properties);
	}
}
