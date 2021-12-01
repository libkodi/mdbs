# 多数据源封装库

### 概述
本库使用了mybatis与PooledDataSource来管理数据库的操作与各个连接池。  
本库创建的PooledDataSource与SqlSessionFactory是完全复用的，以一个databaseId为键名保存，只要键名不变或没有超过指定的闲置时间，使用同一databaseId获取到的PooledDataSource与SqlSessionFactory永远是同一个。  
本库可以完全不需要在application.properties中指定任何配置，数据库的连接信息完全可以在程序运行后动态指定。  

### 注意点
- 本库只封装了多数据源的操作，并没有默认添加各个数据库的驱动包，所有在使用时需要另外在各自的项目中添加需要的数据库驱动包
- 由于是动态创建的数据库连接，在使用mybatis的数据库操作时需要使用xml方式的mapper

### 使用
##### 1.引入
```
<dependency>
    <groupId>io.github.libkodi.mdbs</groupId>
    <artifactId>multi-datasource</artifactId>
    <version>1.0.0</version>
</dependency>
```

##### 2.设置配置信息
*在application.properties中添加如下配置*
```
mdbs.refresh-period=1 # 查找过期闲置间隔(秒), 默认为: 1
mdbs.idle-timeout=3600 # 连接闲置过期时间(秒), 默认为: 3600

# 配置主数据库信息
mdbs.info.primary.url=xxxxxx
mdbs.info.primary.driver=xxxxx
mdbs.info.primary.username=xxxx
mdbs.info.primary.password=xxxx

# 其它连接数据库默认信息
#mdbs.info.xxxx.url=xxxxx
#mdbs.info.xxxx.driver=xxx
#mdbs.info.xxxx.username=xxx
#mdbs.info.xxxx.password=xxx
```

##### 3.创建一个Config文件来实现数据源与会话的初始化处理

```java
/** 
 *  
 * 初始化数据源 
 * 
 * @description 为不同数据源设置连接参数 
 * @return InitialDataSource 
 */ 
@Bean 
public InitialDataSource initialDataSource() { 
    return (databaseId, pool, ctx) -> { 
        if (databaseId.equals("primary")) {
            DataSourceProperty info = ctx.getProperties().getInfo(databaseId);
            pool.setUrl(info.getUrl());
            pool.setDriver(info.getDriver());
            pool.setUsername(info.getUsername());
            pool.setPassword(info.getPassword());
        } else {
            // 1.可以通过主数据库查出连接信息
            // 2.也可以通过properties取出设置好的连接信息
        }
    }; 
} 

/** 
 *  
 * 初始化mybatis设置 
 * 
 * @description 为不同的mybatis连接设置不同的参数和绑定mapper与添加工具之类的 
 * @return InitialSqlSessionFactory 
 */ 
@Bean 
public InitialSqlSessionFactory initialSqlSessionFactory() { 
    return (databaseId, factoryBean, ctx) -> { 
        try { 
            factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:/mapper/*.xml")); 
        } catch(Exception e) { 
            log.error("Failed to load mapper:", e.getMessage()); 
        } 
    }; 
}
```

##### 4. 注入使用
```java
/**
 * 注入数据源管理
 */
@Autowired
private MultiDataSource mdbs;

/**
 * 
 * 调用方法一
 *
 * @description 这种方式打开的SqlSession需要自行在函数中释放连接池的占用
 * @return Object
 */
public Object tables(String databaseId) {
    SqlSession session = null;
    
    try {
        session = mdbs.getSqlSession(databaseId);
        ...
        session.close();
        ...
    } catch (Exception e) {
        log.error("", e.getMessage());
    } finally {
        if (session != null) {
            session.close();
        }
    }
    
    return null;
}

/**
 * 
 * 调用方法二
 *
 * @description 这种方式打开的SqlSession不需要手动释放，在方法内部已经封装了释放处理
 * @return List<Map<String, String>>
 * @throws Exception 
 */
public Object tablesWithCallback(String databaseId) throws Exception {
    return mdbs.openSession(databaseId, (SqlSessionCallback<Object>) session -> {
        ...
    });
}
```

### 手动配置主数据源
```java
@Autowired 
private MultiDataSource mdbs; 

@Bean 
@Primary 
public DataSource getDataSource() { 
    return mdbs.getDataSource("primary"); 
} 

@Bean 
@Primary 
public SqlSessionFactory getSqlSessionFactory() { 
    try { 
        return mdbs.getSqlSessionFactory("primary"); 
    } catch (Exception e) { 
        log.error("Failed to load sqlsessionfactory", e.getMessage()); 
        return null; 
    } 
} 

@Primary 
@Bean 
public DataSourceTransactionManager getTransactionManager(DataSource ds) { 
    return new DataSourceTransactionManager(ds); 
} 

@Primary 
@Bean 
public SqlSessionTemplate getSqlSessionTemplate(SqlSessionFactory factory) { 
    return new SqlSessionTemplate(factory); 
}
```