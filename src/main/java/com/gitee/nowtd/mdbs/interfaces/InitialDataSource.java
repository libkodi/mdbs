package com.gitee.nowtd.mdbs.interfaces;

import org.apache.ibatis.datasource.pooled.PooledDataSource;

import com.gitee.nowtd.mdbs.properties.MultipartDataSourceProperties;

public interface InitialDataSource {
	public void init(String databaseId, PooledDataSource pool, MultipartDataSourceProperties properties);
}
