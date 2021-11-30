package com.github.libkodi.mdbs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.libkodi.mdbs.config.DataSource;
import com.github.libkodi.mdbs.entity.SqlSessionFactoryEntity;
import com.github.libkodi.mdbs.interfaces.InitialDataSource;
import com.github.libkodi.mdbs.interfaces.InitialSqlSessionFactory;
import com.github.libkodi.mdbs.interfaces.SqlSessionCallback;
import com.github.libkodi.mdbs.properties.MultipartDataSourceProperties;

public class MultipartDataSource {
	private static MultipartDataSource instance = null; // 单例
	/** 保存数据源 */
	private HashMap<String, PooledDataSource> dataSourcePool = new HashMap<String, PooledDataSource>();
	/** 保存会话 */
	private HashMap<String, SqlSessionFactoryEntity> factoryPool = new HashMap<String, SqlSessionFactoryEntity>();
	
	@Autowired
	private InitialSqlSessionFactory initialSqlSessionFactory;
	
	@Autowired
	private InitialDataSource initialDataSource;
	
	private MultipartDataSourceProperties properties;
	
	public MultipartDataSourceProperties getProperties() {
		return properties;
	}
	
	/**
	 * 单例的实现
	 */
	public static MultipartDataSource getInstance(MultipartDataSourceProperties properties) {
		if (instance == null) {
			instance = new MultipartDataSource(properties);
		}
		
		return instance;
	}
	
	/**
	 * 构造函数
	 */
	private MultipartDataSource(MultipartDataSourceProperties properties) {
		this.properties = properties;
		
		/**
		 * 开线程处理超时闲置的连接
		 */
		Thread th = new Thread(new Runnable() {
			
			@Override
			public void run() {
				long sleepTime = Math.max(1, properties.getRefreshPeriod()) * 1000;
				
				while (true) {
					try {
						ArrayList<String> keys = new ArrayList<String>();
						
						Iterator<Entry<String, SqlSessionFactoryEntity>> it = factoryPool.entrySet().iterator();
						
						while (it.hasNext()) {
							Entry<String, SqlSessionFactoryEntity> next = it.next();
							
							if (next.getValue().isValid(properties.getIdleTimeout())) {
								keys.add(next.getKey());
							}
						}
						
						if (keys.size() > 0) {
							synchronized (factoryPool) {
								synchronized (dataSourcePool) {
									for (String key : keys) {
										try {
											factoryPool.remove(key);
											dataSourcePool.remove(key).forceCloseAll();
										} catch (Exception e) {}
									}
								}
							}
						}
					} catch (Exception e) {}
					
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {}
				}
			}
		});
		
		th.setDaemon(true);
		th.start();
	}
	
	/**
	 * 判断是否有指定数据源
	 */
	public boolean containsDataSource(String databaseId) {
		synchronized (dataSourcePool) {
			return dataSourcePool.containsKey(databaseId);
		}
	}
	
	/**
	 * 判断是否有指定会话
	 */
	public boolean containsFactory(String databaseId) {
		synchronized (factoryPool) {
			return factoryPool.containsKey(databaseId);
		}
	}
	
	/**
	 * 创建数据源
	 */
	public PooledDataSource getDataSource(String databaseId, DataSource info) {
		synchronized (dataSourcePool) {
			if (dataSourcePool.containsKey(databaseId)) {
				return dataSourcePool.get(databaseId);
			} else {
				PooledDataSource pool = null;
				
				if (info != null && !dataSourcePool.containsKey(databaseId)) {
					pool = new PooledDataSource();
					pool.setDriver(info.getDriver());
					pool.setUrl(info.getUrl());
					pool.setUsername(info.getUsername());
					pool.setPassword(info.getPassword());
					
					if (info.getAutoCommit() != null) {
						pool.setDefaultAutoCommit(info.getAutoCommit());
					}
					
					if (info.getDefaultTransactionIsolationLevel() != null) {
						pool.setDefaultTransactionIsolationLevel(info.getDefaultTransactionIsolationLevel());
					}
					
					if (info.getMaximumActiveConnections() != null) {
						pool.setPoolMaximumActiveConnections(info.getMaximumActiveConnections());
					}
					
					if (info.getLoginTimeout() != null) {
						pool.setLoginTimeout(info.getLoginTimeout());
					}
					
					if (info.getMaximumCheckoutTime() != null) {
						pool.setPoolMaximumCheckoutTime(info.getMaximumCheckoutTime());
					}
					
					if (info.getMaximumIdleConnections() != null) {
						pool.setPoolMaximumIdleConnections(info.getMaximumIdleConnections());
					}
				} else {
					pool = new PooledDataSource();
					initialDataSource.init(databaseId, pool, properties);
				}
				
				dataSourcePool.put(databaseId, pool);
				
				return pool;
			}
		}
	}
	
	public PooledDataSource getDataSource(String databaseId) {
		return getDataSource(databaseId, null);
	}
	
	/**
	 * 创建会话
	 */
	public SqlSessionFactory getSqlSessionFactory(String databaseId, DataSource info) throws Exception {
		synchronized (factoryPool) {
			if (factoryPool.containsKey(databaseId)) {
				SqlSessionFactoryEntity entity = factoryPool.get(databaseId);
				entity.renew();
				return entity.getFactory();
			} else {
				PooledDataSource pool = getDataSource(databaseId, info);
				
				SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
				bean.setDataSource(pool);
				
				if (initialSqlSessionFactory != null) {
					initialSqlSessionFactory.init(databaseId, pool, bean, properties);
				}
				
				SqlSessionFactory factory = bean.getObject();
				SqlSessionFactoryEntity entity = new SqlSessionFactoryEntity();
				entity.setFactory(factory);
				
				factoryPool.put(databaseId, entity);
				
				return factory;
			}
		}
	}
	
	public SqlSessionFactory getSqlSessionFactory(String databaseId) throws Exception {
		return getSqlSessionFactory(databaseId, null);
	}
	
	public SqlSession getSqlSession(String databaseId, DataSource info) throws Exception {
		SqlSessionFactory factory = getSqlSessionFactory(databaseId, info);
		
		if (factory != null) {
			return factory.openSession();
		} else {
			return null;
		}
	}
	
	public SqlSession getSqlSession(String databaseId) throws Exception {
		return getSqlSession(databaseId, null);
	}
	
	public <T> T openSession(String databaseId, SqlSessionCallback<T> callback) throws Exception {
		SqlSession session = null;
		T result = null;
		Exception error = null;
		
		try {
			session = getSqlSession(databaseId);
			result = callback.call(session);
		} catch (Exception e) {
			error = e;
		} finally {
			if (session != null) {
				session.close();
			}
		}
		
		if (error != null) {
			throw error;
		}
		
		return result;
	}
	
	public void close(String databaseId) {
		synchronized (factoryPool) {
			if (factoryPool.containsKey(databaseId)) {
				factoryPool.remove(databaseId);
			}
			
			synchronized (dataSourcePool) {
				if (dataSourcePool.containsKey(databaseId)) {
					dataSourcePool.remove(databaseId).forceCloseAll();
				}
			}
		}
	}
	
	public void closeAll() {
		synchronized (dataSourcePool) {
			synchronized (factoryPool) {
				Iterator<Entry<String, PooledDataSource>> iter = dataSourcePool.entrySet().iterator();
				
				while (iter.hasNext()) {
					iter.next().getValue().forceCloseAll();
				}
				
				dataSourcePool.clear();
				factoryPool.clear();
			}
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		closeAll();
	}
}
