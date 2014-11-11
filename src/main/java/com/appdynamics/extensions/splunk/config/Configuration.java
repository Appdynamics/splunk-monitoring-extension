package com.appdynamics.extensions.splunk.config;

import java.util.HashSet;
import java.util.Set;

public class Configuration {

	private String host;
	private int port;
	private String username;
	private String password;
	private boolean usessl;
	private Set<String> searchKeywords = new HashSet<String>();
	private String metricPrefix;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean getUsessl() {
		return usessl;
	}

	public void setUsessl(boolean usessl) {
		this.usessl = usessl;
	}

	public String getMetricPrefix() {
		return metricPrefix;
	}

	public void setMetricPrefix(String metricPrefix) {
		this.metricPrefix = metricPrefix;
	}

	public Set<String> getSearchKeywords() {
		return searchKeywords;
	}

	public void setSearchKeywords(Set<String> searchKeywords) {
		this.searchKeywords = searchKeywords;
	}
}
