package io.github.libkodi.mdbs.interfaces;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;

import io.github.libkodi.mdbs.properties.MultipartDataSourceProperties;

public interface InitialSqlSessionFactory {
	public void init(String databaseId, PooledDataSource pool, SqlSessionFactoryBean factoryBean, MultipartDataSourceProperties properties);
}
