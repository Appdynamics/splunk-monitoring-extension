package com.appdynamics.extensions.splunk;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.splunk.common.Authenticator;
import com.appdynamics.extensions.splunk.config.Configuration;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class SplunkMonitor extends AManagedMonitor {

	private static Logger logger = Logger.getLogger("com.singularity.extensions.SplunkMonitor");
	public static final String CONFIG_ARG = "config-file";
	public static final String METRIC_SEPARATOR = "|";
	private static final int DEFAULT_NUMBER_OF_THREADS = 5;

	private ExecutorService threadPool;

	public SplunkMonitor() {
		String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		System.out.println(msg);
	}

	public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext arg1) throws TaskExecutionException {
		if (taskArguments != null) {
			logger.info("Starting the Splunk Monitoring Task");
			String configFilename = getConfigFilename(taskArguments.get(CONFIG_ARG));
			try {
				final Configuration config = YmlReader.readFromFile(configFilename, Configuration.class);
				final Map<String, String> clientArguments = buildHttpClientArguments(config);

				SimpleHttpClient httpClient = SimpleHttpClient.builder(clientArguments).build();
				final String authToken = new Authenticator().getAuthToken(config, httpClient);
				threadPool = Executors.newFixedThreadPool(config.getNumberOfThreads() == 0 ? DEFAULT_NUMBER_OF_THREADS : config.getNumberOfThreads());

				CompletionService ecs = new ExecutorCompletionService(threadPool);
				int count = 0;
				for (final String keyWord : config.getSearchKeywords()) {
					ecs.submit(new Callable() {
						public Object call() throws Exception {
							fetchAndPrintMetrics(clientArguments, config, authToken, keyWord);
							return null;
						}
					});
					++count;
				}
				for (int i = 0; i < count; i++) {
					ecs.take().get();
				}
				logger.info("Splunk monitoring task completed successfully.");
				return new TaskOutput("Splunk monitoring task completed successfully.");
			} catch (Exception e) {
				logger.error("Metrics collection failed", e);
			} finally {
				if (!threadPool.isShutdown()) {
					threadPool.shutdown();
				}
			}
		}
		throw new TaskExecutionException("Splunk monitoring task completed with failures.");
	}

	private Map<String, String> buildHttpClientArguments(Configuration config) {
		Map<String, String> clientArgs = new HashMap<String, String>();
		clientArgs.put(TaskInputArgs.USE_SSL, Boolean.toString(config.getUsessl()));
		clientArgs.put(TaskInputArgs.HOST, config.getHost());
		clientArgs.put(TaskInputArgs.PORT, String.valueOf(config.getPort()));
		clientArgs.put(TaskInputArgs.USER, config.getUsername());
		clientArgs.put(TaskInputArgs.PASSWORD, config.getPassword());
		return clientArgs;
	}

	private void fetchAndPrintMetrics(Map<String, String> clientArguments, Configuration config, String authToken, String keyWord) {
		SimpleHttpClient httpClient = SimpleHttpClient.builder(clientArguments).build();
		SplunkMonitorTask monitorTask = new SplunkMonitorTask();
		Map<String, String> metrics = monitorTask.processSplunkMetrics(httpClient, authToken, keyWord);
		printMetrics(config, metrics);
	}

	private void printMetrics(Configuration config, Map<String, String> metrics) {
		String metricPrefix = config.getMetricPrefix();
		for (Map.Entry<String, String> metric : metrics.entrySet()) {
			StringBuilder metricPath = new StringBuilder();
			metricPath.append(metricPrefix).append(metric.getKey());
			printMetric(metricPath.toString(), metric.getValue());
		}
	}

	private void printMetric(String metricPath, String metricValue) {
		printMetric(metricPath, metricValue, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
	}

	private void printMetric(String metricPath, String metricValue, String aggType, String timeRollupType, String clusterRollupType) {
		MetricWriter metricWriter = getMetricWriter(metricPath, aggType, timeRollupType, clusterRollupType);
		if (metricValue != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType + "] metric = "
						+ metricPath + " = " + metricValue);
			}
			metricWriter.printMetric(metricValue);
			System.out.println("metric = " + metricPath + " = " + metricValue);
		}
	}

	private String getConfigFilename(String filename) {
		if (filename == null) {
			return "";
		}
		// for absolute paths
		if (new File(filename).exists()) {
			return filename;
		}
		// for relative paths
		File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
		String configFileName = "";
		if (!Strings.isNullOrEmpty(filename)) {
			configFileName = jarPath + File.separator + filename;
		}
		return configFileName;
	}

	private static String getImplementationVersion() {
		return SplunkMonitor.class.getPackage().getImplementationTitle();
	}

	public static void main(String[] args) throws TaskExecutionException {
		Map<String, String> taskArguments = new HashMap<String, String>();
		taskArguments.put(CONFIG_ARG,
				"/home/balakrishnav/AppDynamics/ExtensionsProject/splunk-monitoring-extension/src/main/resources/conf/config.yml");
		SplunkMonitor monitor = new SplunkMonitor();
		monitor.execute(taskArguments, null);

	}
}
