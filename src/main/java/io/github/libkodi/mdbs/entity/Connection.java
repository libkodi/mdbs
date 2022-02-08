package io.github.libkodi.mdbs.entity;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSessionFactory;

import lombok.Data;

@Data
public class Connection {
	private PooledDataSource pooledDataSource;
	private SqlSessionFactory sqlSessionFacroty;
	private int maxIdleTime = 3600;
}
