package io.github.libkodi.mdbs.properties;

import java.util.HashMap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.github.libkodi.mdbs.config.DataSourceProperty;
import lombok.Data;

@Data
@ConfigurationProperties(prefix = "mdbs")
public class MultiDataSourceProperties {
	private int refreshPeriod = 1;
	private int maxIdleTime = 3600;
	private HashMap<String, DataSourceProperty> info;
	
	public DataSourceProperty getInfo(String key) {
		return info.get(key);
	}
}
