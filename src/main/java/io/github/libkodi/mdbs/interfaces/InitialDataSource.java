package io.github.libkodi.mdbs.interfaces;

import org.apache.ibatis.datasource.pooled.PooledDataSource;

import io.github.libkodi.mdbs.properties.MultipartDataSourceProperties;

public interface InitialDataSource {
	public void init(String databaseId, PooledDataSource pool, MultipartDataSourceProperties properties);
}
