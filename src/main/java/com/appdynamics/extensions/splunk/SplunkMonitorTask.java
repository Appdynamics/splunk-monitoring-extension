package com.appdynamics.extensions.splunk;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.http.WebTarget;
import com.appdynamics.extensions.splunk.config.SearchKeyword;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
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

	private static final String AMPERSAND = "&";
	private static final String SPACE = " ";
	private static final Logger logger = Logger.getLogger(SplunkMonitorTask.class);
	private static final String QUERY_ENDPOINT_URI = "/servicesNS/admin/search/search/jobs/export";
	public static final String METRIC_SEPARATOR = "|";

	private SimpleHttpClient httpClient;
	private String authToken;
	private SearchKeyword keyword;

	public SplunkMonitorTask(SimpleHttpClient httpClient, String authToken, SearchKeyword keyword) {
		this.httpClient = httpClient;
		this.authToken = authToken;
		this.keyword = keyword;
	}

	public SplunkMetrics call() throws Exception {
		SplunkMetrics splunkMetrics = new SplunkMetrics();
		Map<String, String> metrics = Maps.newHashMap();
		Response response = null;
		try {
			// queries Splunk to get event count for the earlier minute
			// to avoid indexing lag and accurate value
			long currentTime = System.currentTimeMillis() / 1000;
			long toTime = currentTime - currentTime % 60;
			long fromTime = toTime - 60;

			WebTarget target = httpClient.target().path(QUERY_ENDPOINT_URI);
			target.header("Authorization", authToken);
			String query = buildQuery(keyword, fromTime, toTime);
			response = target.post(query);
			if (logger.isDebugEnabled()) {
				logger.debug("Queried Splunk for " + keyword + " to retrieve eventcount for the time period " + new Date(fromTime * 1000) + " to "
						+ new Date(toTime * 1000));
				logger.debug("Query is " + query);
			}
			JsonNode node = new ObjectMapper().readValue(response.string(), JsonNode.class);
			String value = node.findValue("count").asText();
			String metricName = Strings.isNullOrEmpty(keyword.getDisplayName()) ? keyword.getKeyword() : keyword.getDisplayName();
			metrics.put(metricName, value);
			splunkMetrics.setMetrics(metrics);
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

	public String buildQuery(SearchKeyword keyword, long fromTime, long toTime) {
		StringBuilder query = new StringBuilder("search=search ");
		query.append(Strings.isNullOrEmpty(keyword.getKeyword()) ? "" : keyword.getKeyword() + SPACE);
		query.append(Strings.isNullOrEmpty(keyword.getHost()) ? "" : "host=" + keyword.getHost() + SPACE);
		query.append(Strings.isNullOrEmpty(keyword.getSource()) ? "" : "source=" + keyword.getSource() + SPACE);
		query.append(Strings.isNullOrEmpty(keyword.getSourcetype()) ? "" : "sourcetype=" + keyword.getSourcetype() + SPACE);
		query.append(Strings.isNullOrEmpty(keyword.getIndex()) ? "" : "index=" + keyword.getIndex() + SPACE);
		query.append("| stats count" + AMPERSAND + "output_mode=json");
		query.append(AMPERSAND + "earliest_time=" + fromTime + AMPERSAND + "latest_time=" + toTime);
		return query.toString();
	}
}
