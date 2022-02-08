package io.github.libkodi.mdbs.interfaces;

import org.mybatis.spring.SqlSessionFactoryBean;

import io.github.libkodi.mdbs.MultiDataSource;

public interface InitialSqlSessionFactory {
	public void init(MultiDataSource context, String id, SqlSessionFactoryBean bean);
}
