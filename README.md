# Splunk Monitoring Extension

This extension works only with the standalone machine agent.

## Use Case
Splunk captures, indexes and correlates real-time data in a searchable repository from which it can generate graphs, reports, alerts, dashboards and visualizations.
Splunk Monitoring extension reports event count to AppDynamics Controller for a keyword per minute basis.

##Installation
1. Run 'mvn clean install' from the splunk-monitoring-extension directory and find the SplunkMonitor.zip in the "target" folder.
2. Unzip as "SplunkMonitor" and copy the "SplunkMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`

## Configuration ##
Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the Splunk extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/SplunkMonitor/`. Specify the host, port, username, password of Splunk server.

   For eg.
   ```
        # Splunk Server particulars
		host: "localhost"
		# Splunkd Management port
		port: 8089
		username: "admin"
		password: "admin"
		usessl: true

		searchKeywords: [
                  "controller",
                  "thread"
                ]

		# number of concurrent tasks
		numberOfThreads: 5

		#prefix used to show up metrics in AppDynamics
		metricPrefix:  "Custom Metrics|Splunk|"

   ```

3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/SplunkMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/SplunkMonitor/config.yml" />
          ....
     </task-arguments>
    ```

## Metrics

* Count (event count for a keyword)

In addition to the above metrics, we also add a metric called "Metrics Collection Successful" with a value 0 when an error occurs and 1 when the metrics collection is successful.

Note : By default, a Machine agent or a AppServer agent can send a fixed number of metrics to the controller. To change this limit, please follow the instructions mentioned [here](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
For eg.  
```    
    java -Dappdynamics.agent.maxMetrics=2500 -jar machineagent.jar
```

## Custom Dashboard
![]()

## Contributing
Always feel free to fork and contribute any changes directly here on GitHub.

##Community
Find out more in the [AppSphere]() community.

##Support
For any questions or feature request, please contact [AppDynamics Support](mailto:help@appdynamics.com). 

