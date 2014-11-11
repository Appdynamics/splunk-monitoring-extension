package com.appdynamics.extensions.splunk.common;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.http.WebTarget;
import com.appdynamics.extensions.splunk.config.Configuration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Authenticator {

	private static final Logger logger = Logger.getLogger("com.singularity.extensions.Authenticator");
	private static final String httpPostParam = "username=%s&password=%s&output_mode=json";
	private static final String AUTH_URI = "/servicesNS/admin/search/auth/login/";
	private String authToken;

	public String getAuthToken(Configuration config, SimpleHttpClient httpClient) {
		Response response = null;
		try {
			response = postAuthenticationRequest(config, httpClient);
			JsonNode jsonNode = parseAuthenticationResponse(response);
			int statusCode = response.getStatus();
			if (statusCode != 200) {
				String type = jsonNode.findValue("type").asText();
				String errorMessage = jsonNode.findValue("text").asText();
				logger.error("Error while Authenticating " + type + " " + errorMessage);
				throw new RuntimeException("Error while Authenticating " + type + " " + errorMessage);
			}
			authToken = jsonNode.findValue("sessionKey").asText();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return authToken;
	}

	private JsonNode parseAuthenticationResponse(Response response) {
		try {
			JsonNode node = new ObjectMapper().readValue(response.inputStream(), JsonNode.class);
			return node;
		} catch (Exception e) {
			logger.error(e);
			throw new RuntimeException(e);
		}

	}

	private Response postAuthenticationRequest(Configuration config, SimpleHttpClient httpClient) {
		WebTarget target = httpClient.target().path(AUTH_URI);
		String data = String.format(httpPostParam, config.getUsername(), config.getPassword());
		Response response = target.post(data);
		return response;
	}
}
