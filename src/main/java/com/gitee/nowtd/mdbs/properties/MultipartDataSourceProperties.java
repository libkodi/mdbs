package com.gitee.nowtd.mdbs.properties;

import java.util.HashMap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.gitee.nowtd.mdbs.config.DataSource;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "mdbs")
public class MultipartDataSourceProperties {
	private int refreshPeriod = 1;
	private long idleTimeout = 3600;
	private HashMap<String, DataSource> info;
	
	public DataSource getInfo(String key) {
		return info.get(key);
	}
}
