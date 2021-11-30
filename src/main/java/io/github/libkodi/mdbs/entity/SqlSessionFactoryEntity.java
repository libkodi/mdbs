package io.github.libkodi.mdbs.entity;

import org.apache.ibatis.session.SqlSessionFactory;

import lombok.Data;

@Data
public class SqlSessionFactoryEntity {
	private long timestamp;
	private SqlSessionFactory factory;
	
	public SqlSessionFactoryEntity() {
		renew();
	}
	
	public void renew() {
		timestamp = System.currentTimeMillis();
	}
	
	public boolean isValid(long timeout) {
		long diff = System.currentTimeMillis() - timestamp;	
		return (diff / 1000) >= timeout;
	}
}
