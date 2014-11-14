/**
 * Copyright 2014 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.extensions.splunk.common;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.http.WebTarget;
import com.appdynamics.extensions.splunk.config.Configuration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Authenticator {

	private static final Logger logger = Logger.getLogger(Authenticator.class);
	private static final String httpPostParam = "username=%s&password=%s&output_mode=json";
	private static final String AUTH_URI = "/servicesNS/admin/search/auth/login/";

	public String getAuthToken(Configuration config, SimpleHttpClient httpClient) {
		Response response = null;
		String authToken;
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
			logger.error(e);
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
