package com.appdynamics.extensions.splunk;

import java.util.Map;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.http.WebTarget;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

public class SplunkMonitorTask {

	private static final Logger logger = Logger.getLogger(SplunkMonitorTask.class);
	private static final String QUERY_ENDPOINT_URI = "/servicesNS/admin/search/search/jobs/export";
	private static final String QUERY = "search=search %s | stats count by sourcetype&earliest_time=-1@m&output_mode=json";
	public static final String METRIC_SEPARATOR = "|";

	public Map<String, String> processSplunkMetrics(SimpleHttpClient httpClient, String authToken, String keyword) {
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
		return metrics;
	}

}
