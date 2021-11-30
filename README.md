# 多数据源封装库

### 功能
- 使用mybatis来处理所有连接
- 使用PooledDataSource自动管理连接池
- 动态创建和获取与删除(关闭)
- 自动移除超时的闲置连接

### 使用
##### 1.下载本源码
```
git clone https://gitee.com/nowtd/mdbs.git
```

##### 2.引入
```
<dependency>
    <groupId>com.gitee.nowtd</groupId>
    <artifactId>multipart-datasource</artifactId>
    <version>1.0.0</version>
</dependency>
```

##### 3.设置配置信息
*在application.properties中添加如下配置*
```
mdbs.refresh-period=1 # 查找过期闲置间隔(秒)
mdbs.idle-timeout=3600 # 连接闲置过期时间(秒)

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

##### 4.创建一个Config文件来实现数据源与会话的初始化处理

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.gitee.nowtd.mdbs.config.DataSource;
import com.gitee.nowtd.mdbs.interfaces.InitialDataSource;
import com.gitee.nowtd.mdbs.interfaces.InitialSqlSessionFactory;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MultiDataSourceConfiguration {
    /**
     * 
     * 初始化数据源
     *
     * @description 为不同数据源设置连接参数
     * @return InitialDataSource
     */
    @Bean
    public InitialDataSource initialDataSource() {
        return (databaseId, pool, properties) -> {
            if (databaseId.equals("primary")) {
                DataSource info = properties.getInfo(databaseId);
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
    
    private static Resource[] mappers = null;
    
    /**
     * 
     * 初始化mybatis设置
     *
     * @description 为不同的mybatis连接设置不同的参数和绑定mapper与添加工具之类的
     * @return InitialSqlSessionFactory
     */
    @Bean
    public InitialSqlSessionFactory initialSqlSessionFactory() {
        return (databaseId, pool, factoryBean, properties) -> {
            try {
                if (mappers == null) {
                    mappers = new PathMatchingResourcePatternResolver().getResources("classpath*:/mapper/*.xml");
                }
                
                factoryBean.setMapperLocations(mappers);
            } catch(Exception e) {
                log.error("Failed to load mapper:", e.getMessage());
            }
        };
    }
}


```

##### 5. 注入使用
```java
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gitee.nowtd.mdbs.MultipartDataSource;
import com.gitee.nowtd.mdbs.interfaces.SqlSessionCallback;
import com.test.dao.TestMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TestService {
    /**
     * 注入数据源管理
     */
    @Autowired
    private MultipartDataSource mdbs;
    
    /**
     * 
     * 获取数据库下的所有表
     *
     * @description 这种方式打开的SqlSession需要自行在函数中关闭翻译连接池的占用
     * @return List<Map<String, String>>
     */
    public List<Map<String, String>> tables(String databaseId) {
        SqlSession session = null;
        
        try {
            session = mdbs.getSqlSession(databaseId);
            TestMapper testMaper = session.getMapper(TestMapper.class);
            List<Map<String, String>> res = testMaper.tables();
            session.close();
            return res;
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
     * 获取数据库下的所有表
     *
     * @description 这种方式打开的SqlSession不需要手动释放，在方法内部已经封装了释放处理
     * @return List<Map<String, String>>
     * @throws Exception 
     */
    public List<Map<String, String>> tablesWithCallback(String databaseId) throws Exception {
        return mdbs.openSession(databaseId, (SqlSessionCallback<List<Map<String, String>>>) session -> {
            TestMapper testMaper = session.getMapper(TestMapper.class);
            return testMaper.tables();
        });
    }
}

```

### 以本项目为spring boot项目配置主数据库(非直接在application.properties中的设置)
```java
import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.gitee.nowtd.mdbs.MultipartDataSource;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DatabaseConfiguration {
    @Autowired
    private MultipartDataSource mdbs;
    
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
    public DataSourceTransactionManager getTransactionManager() {
        return new DataSourceTransactionManager(mdbs.getDataSource("primary"));
    }
    
    @Primary
    @Bean
    public SqlSessionTemplate getSqlSessionTemplate() {
        try {
            return new SqlSessionTemplate(mdbs.getSqlSessionFactory("primary"));
        } catch (Exception e) {
            log.error("Failed to load SqlSessionTemplate", e.getMessage());
            return null;
        }
    }
}

```