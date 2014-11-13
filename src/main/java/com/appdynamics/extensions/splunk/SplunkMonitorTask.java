package com.appdynamics.extensions.splunk;

import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.http.WebTarget;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

/**
 * Copyright 2014 AppDynamics, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
public class SplunkMonitorTask implements Callable<SplunkMetrics> {

	private static final Logger logger = Logger.getLogger(SplunkMonitorTask.class);
	private static final String QUERY_ENDPOINT_URI = "/servicesNS/admin/search/search/jobs/export";
	private static final String QUERY = "search=search %s | stats count by sourcetype&earliest_time=-1@m&output_mode=json";
	public static final String METRIC_SEPARATOR = "|";

	private SimpleHttpClient httpClient;
	private String authToken;
	private String keyword;

	public SplunkMonitorTask(SimpleHttpClient httpClient, String authToken, String keyword) {
		this.httpClient = httpClient;
		this.authToken = authToken;
		this.keyword = keyword;
	}

	public SplunkMetrics call() throws Exception {
		SplunkMetrics splunkMetrics = new SplunkMetrics();
		Map<String, String> metrics = Maps.newHashMap();
		Response response = null;
		try {
			WebTarget target = httpClient.target().path(QUERY_ENDPOINT_URI);
			target.header("Authorization", authToken);
			String data = String.format(QUERY, keyword);
			response = target.post(data);
			String[] lines = response.string().split(System.getProperty("line.separator"));
			for (String line : lines) {
				JsonNode node = new ObjectMapper().readValue(line, JsonNode.class);
				if (lines.length == 1 && node.findValue("sourcetype") == null) {
					logger.info("No events found for search keyword " + keyword);
					break;
				}
				if (node.findValue("sourcetype") != null) {
					String sourceType = node.findValue("sourcetype").asText();
					String value = node.findValue("count").asText();
					String metricPath = keyword + METRIC_SEPARATOR + sourceType + METRIC_SEPARATOR + "count";
					metrics.put(metricPath, value);
					splunkMetrics.setMetrics(metrics);
				}
			}
		} catch (Exception e) {
			logger.error("Error in querying count for " + keyword, e);
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return splunkMetrics;
	}
}
