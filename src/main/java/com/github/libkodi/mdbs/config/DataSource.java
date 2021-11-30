package com.github.libkodi.mdbs.config;

import lombok.Data;

@Data
public class DataSource {
	private String driver;
	private String url;
	private String username;
	private String password;
	private Integer defaultTransactionIsolationLevel = null;
	private Boolean autoCommit = null;
	private Integer maximumActiveConnections = null;
	private Integer loginTimeout = null;
	private Integer maximumCheckoutTime = null;
	private Integer maximumIdleConnections = null;
}
