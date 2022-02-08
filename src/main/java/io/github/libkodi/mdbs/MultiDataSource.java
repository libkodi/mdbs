package io.github.libkodi.mdbs;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.libkodi.mdbs.config.DataSourceProperty;
import io.github.libkodi.mdbs.entity.Connection;
import io.github.libkodi.mdbs.interfaces.InitialDataSource;
import io.github.libkodi.mdbs.interfaces.InitialSqlSessionFactory;
import io.github.libkodi.mdbs.interfaces.Callback;
import io.github.libkodi.mdbs.properties.MultiDataSourceProperties;
import io.github.libkodi.objectlist.withexpires.PeriodMap;
import io.github.libkodi.objectlist.withexpires.PeriodMapNode;

public class MultiDataSource {
	private static MultiDataSource instance = null; // 单例
	private PeriodMap<Connection> data = new PeriodMap<Connection>();
	private Object mutex;
	
	@Autowired
	private InitialSqlSessionFactory initialSqlSessionFactory;
	
	@Autowired
	private InitialDataSource initialDataSource;
	
	private MultiDataSourceProperties properties;
	
	public MultiDataSourceProperties getProperties() {
		return properties;
	}
	
	/**
	 * 单例的实现
	 */
	public static MultiDataSource getInstance(MultiDataSourceProperties properties) {
		if (instance == null) {
			instance = new MultiDataSource(properties);
		}
		
		return instance;
	}
	
	/**
	 * 构造函数
	 */
	private MultiDataSource(MultiDataSourceProperties properties) {
		this.properties = properties;
		mutex = this;
		
		/**
		 * 开线程处理超时闲置的连接
		 */
		Thread th = new Thread(new Runnable() {
			
			@Override
			public void run() {
				long sleepTime = Math.max(1, properties.getRefreshPeriod()) * 1000;
				
				while (true) {
					try {
						data.update();
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
	 * 
	 * 判断是否有指定数据源
	 *
	 * @param id 数据源ID
	 * @return true/false
	 */
	public boolean containsDataSource(String id) {
		synchronized (mutex) {
			if (data.containsKey(id)) {
				Connection conn = data.get(id);
				return conn.getPooledDataSource() != null;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * 
	 * 判断是否有指定会话
	 *
	 * @param id 数据源ID
	 * @return true/false
	 */
	public boolean containsFactory(String id) {
		synchronized (mutex) {
			if (data.containsKey(id)) {
				Connection conn = data.get(id);
				return conn.getSqlSessionFacroty() != null;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * 
	 * 创建数据源
	 *
	 * @param id 数据源ID
	 * @param info 连接信息
	 * @return PooledDataSource
	 */
	public PooledDataSource getDataSource(String id, DataSourceProperty info) {
		synchronized (mutex) {
			return __getDataSource(id, info);
		}
	}
	
	/**
	 * 
	 * 创建数据源
	 *
	 * @param id 数据源ID
	 * @param info 连接信息
	 * @return PooledDataSource
	 */
	private PooledDataSource __getDataSource(String id, DataSourceProperty info) {
		if (data.containsKey(id)) {
			Connection conn = data.get(id);
			
			if (conn != null) {
				return conn.getPooledDataSource();
			} else {
				return null;
			}
		} else {
			Connection conn = new Connection();
			PooledDataSource pool = null;
			
			if (info != null && !data.containsKey(id)) {
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
				
				conn.setPooledDataSource(pool);
				conn.setMaxIdleTime(properties.getMaxIdleTime());
			} else {
				pool = new PooledDataSource();
				conn.setPooledDataSource(pool);
				conn.setMaxIdleTime(properties.getMaxIdleTime());
				initialDataSource.init(this, id, conn);
			}
			
			data.put(id, conn, conn.getMaxIdleTime(), conn.getMaxIdleTime());
			
			return pool;
		}
	}
	
	/**
	 * 
	 * 创建数据源
	 *
	 * @param id 数据源ID
	 * @return PooledDataSource
	 */
	public PooledDataSource getDataSource(String id) {
		return getDataSource(id, null);
	}
	
	/**
	 * 
	 * 创建会话工厂
	 *
	 * @param id 数据源ID
	 * @param info 连接信息
	 * @return SqlSessionFactory
	 * @throws Exception
	 */
	public SqlSessionFactory getSqlSessionFactory(String id, DataSourceProperty info) throws Exception {
		synchronized (mutex) {
			if (data.containsKey(id)) {
				Connection conn = data.get(id);
				
				return createSqlSessionFactory(id, info, conn);
			} else {
				return createSqlSessionFactory(id, info, null);
			}
		}
	}
	
	/**
	 * 
	 * 创建会话工厂
	 *
	 * @param id 数据源ID
	 * @param info 连接信息
	 * @param conn Connection对象
	 * @return SqlSessionFactory
	 * @throws Exception
	 */
	private SqlSessionFactory createSqlSessionFactory(String id, DataSourceProperty info, Connection conn) throws Exception {
		PooledDataSource pool = __getDataSource(id, info);
		
		if (conn == null) {
			conn = data.get(id);
		}
		
		if (conn == null) {
			return null;
		}
		
		SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
		bean.setDataSource(pool);
		
		if (initialSqlSessionFactory != null) {
			initialSqlSessionFactory.init(this, id, bean);
		}
		
		SqlSessionFactory factory = bean.getObject();
		conn.setSqlSessionFacroty(factory);
		
		return factory;
	}
	
	/**
	 * 
	 * 创建会话工厂
	 *
	 * @param id 数据源ID
	 * @return SqlSessionFactory
	 * @throws Exception
	 */
	public SqlSessionFactory getSqlSessionFactory(String id) throws Exception {
		return getSqlSessionFactory(id, null);
	}
	
	/**
	 * 
	 * 创建会话连接
	 *
	 * @param id 数据源ID
	 * @param info 连接信息
	 * @return SqlSession
	 * @throws Exception
	 */
	public SqlSession getSqlSession(String id, DataSourceProperty info) throws Exception {
		SqlSessionFactory factory = getSqlSessionFactory(id, info);
		
		if (factory != null) {
			return factory.openSession();
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * 创建会话连接
	 *
	 * @param id 数据源ID
	 * @return SqlSession
	 * @throws Exception
	 */
	public SqlSession getSqlSession(String id) throws Exception {
		return getSqlSession(id, null);
	}
	
	/**
	 * 
	 * 打开会话并调用回调，自关闭
	 *
	 * @param id 数据源ID
	 * @return 执行结果
	 * @throws Exception
	 */
	public <T> T openSession(String id, Callback<T> callback) throws Exception {
		SqlSession session = null;
		T result = null;
		Exception error = null;
		
		try {
			session = getSqlSession(id);
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
	
	/**
	 * 
	 * 关闭数据源连接
	 *
	 * @param id 数据源ID
	 */
	public void close(String id) {
		synchronized (mutex) {
			if (data.containsKey(id)) {
				Connection conn = data.remove(id);
				
				if (conn != null) {
					PooledDataSource pool = conn.getPooledDataSource();
					
					if (pool != null) {
						pool.forceCloseAll();
					}
				}
			}
		}
	}
	
	/**
	 * 关闭所有数据源连接
	 */
	public void closeAll() throws Exception {
		synchronized (mutex) {
			Iterator<Entry<String, PeriodMapNode<Connection>>> iter = data.iterator();
			
			while(iter.hasNext()) {
				Entry<String, PeriodMapNode<Connection>> next = iter.next();
				
				PeriodMapNode<Connection> node = next.getValue();
				Connection conn = node.getValue();
				conn.getPooledDataSource().forceCloseAll();
			}
			
			data.clear();
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		closeAll();
	}
}
