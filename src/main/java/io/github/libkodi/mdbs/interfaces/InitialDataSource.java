package io.github.libkodi.mdbs.interfaces;

import org.apache.ibatis.datasource.pooled.PooledDataSource;

import io.github.libkodi.mdbs.MultiDataSource;

public interface InitialDataSource {
	public void init(String databaseId, PooledDataSource pool, MultiDataSource context);
}
