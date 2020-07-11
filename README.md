####CURRENCY CONVERSION APP BASED ON MICRO SERVICES
##### Implemented below Spring Cloud solutions 
* Spring Cloud Config for dynamic config changes without restarting the applications
* Feign for http communication between micro services 
* Ribbon for Client Side Load Balancing
* Eureka Naming Server for Service Discovery and Registration
* Zuul Api Gateway Server for implementing common functions across all micro services 
* Sleuth for generating unique request id in the logs for each individual request
* Zipkin for tracing and viewing all the logs of distributed micro services in a single place  
* Spring Cloud Bus for propagate events to group of micro services
* Hystrix Circuit breaker for achieving Fault Tolerance  

**Application**|**Port**
-----------|------------
Limits Service						|8080, 8081
Spring Cloud Config Server			|8888
Currency Exchange Service			|8000, 8001
Currency Conversion Service			|8100, 8101
Currency Eureka Naming Server		|8761
Netflix Zuul API Gateway Server		|8765
Zipkin Distributed Tracing Server	|9411

#####Spring Cloud Config Server:
**sample**: http://localhost:port/propertyname/default  
**actual**: http://localhost:8888/limits-service/default

While connecting spring cloud config server from limits-service we need to add spring cloud config uri in application.properties 
of limits-service. Access multiple properties from config server using the spring profile in the limits-service  
**For multiple environments:**
http://localhost:8888/limits-service/dev
http://localhost:8888/limits-service/qa

Run multiple instances of CurrencyExchangeService by adding VM arguments to **-Dserver.port=8001** in the run configuration

**H2 database:**  
Configured in CurrencyExchangeService
To view the console of H2 we can add the config in application.properties
spring.h2.console.enabled=true  
http://localhost:8000/h2-console/

While running two instance with same H2 DB connection we will get already accessing error. For that we need to add below config in the application.properties
spring.datasource.url=jdbc:h2:file:F:/MyWorks/Spring/db-h2/currency-exchange;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE  

**RestTemplate:**  
Connected CurrencyExchangeService from CurrencyConversionService using traditional RestTemplate approach with the hard coded URL. Instead of using RestTemplate, we will use
Feign Client which is an abstraction layer written on top of RestTemplate.

**Feign:**  
open-feign will remove boiler plate of RestTemplate approach, we need to use **@EnableFeignClients** in spring boot application and 
**@FeignClient** with name and URL of exchange service to the newly create proxy class(below config)  
@FeignClient(name = "currency-exchange-service", url = "localhost:8001")

**Ribbon: Client Side Load Balancing**  
While implementing Ribbon for load balancing we can remove the hard coded URL in the feign proxy class.
We just need to use @RibbonClient with name and add the list of servers (like below) to call in the application.properties  
currency-exchange-service.ribbon.listOfServers=http://localhost:8000,http://localhost:8001  
With this config we can remove the hard coded URL from @FeignClient, now Ribbon will distribute the load across those two CurrencyExchangeServices while calling the feign proxy class from CurrencyConversionService.

//@FeignClient(name = "currency-exchange-service", url = "localhost:8001")  
@FeignClient(name = "currency-exchange-service")  
@RibbonClient(name = "currency-exchange-service")  

**Eureka Naming Server: Service Registration and Service Discovery**  
We need to create new component CurrencyEurekaNamingServer and add @EnableEurekaServer in spring boot application class.
By default Eureka Server port is 8761. Once the application is started we can able to view the Eureka dashboard in http://localhost:8761/.
Also we can stop registering this CurrencyEurekaNamingServer service in Eureka Server by adding the below config in application.properties  
eureka.client.register-with-eureka=false  
eureka.client.fetch-registry=false

**Eureka Client:**  
We can add the annotation @EnableDiscoverClient in each of the spring boot applications of Currency related applications,
which will by default register all the instances of applications to the Eureka server. If we want to configure the 
Eureka Server URL explicitly we can configure in application.properties like below.  
eureka.client.service-url.default-zone=http://localhost:8761

LimitsService, CurrencyExchangService, CurrencyConversionService all registered with Eureka Server by doing the above steps.

Now we can remove the **listOfServers** config for the Ribbon we did in CurrencyConversionService because Ribbon will 
use the Eureka Server to find out the list of instances available to distribute the load balance. Ribbon will use the name we registered 
in the @RibbonClient annotation and refer with Eureka server to fetch the list of instances. 
(commented this config from application.properties)
#######currency-exchange-service.ribbon.listOfServers=http://localhost:8000,http://localhost:8001

**Zuul: Api Gateway**  
Api Gateway is used to implement common functionality across all the micro services. (like Spring AOP used for single service)
1) Authentication, Authorization and Security
2) Logging
3) Aggregating group of services

We are creating new component currency-zuul-api-gateway-server with ZUUL as dependency, and 
add **@EnableZuulProxy** to the spring boot application class and also register this service class with Eureka Server to
make a request using the service name of the ZUUL.

Once ZUUL is registered, we need to extend **ZuulFilter** to log the request or response during the communication between the micro services.
CurrencyExchangService URL will be  http://localhost:8001/currency-exchange/from/USD/to/INR  
When we redirect services via Zuul we need to call this service by using the application name like below.  
**http://localhost:8765/{application-name}/{URI}**
http://localhost:8765/currency-exchange-service/currency-exchange/from/USD/to/INR  
Now the currency exchange request will be logged in Zuul Api Gateway component, likewise we can implement 
security, authentication etc., for all other services in the Zuul Api Gateway server component.
 
If we want to consume the CurrencyExchangService from CurrencyConversionService we need to do the following
changes to the CurrencyExchangeServiceProxy we created early.  
**1st Change:** We need to point to ZUUL api gateway in Feign Proxy class  
//@FeignClient(name = "currency-exchange-service")  
@FeignClient(name = "currency-zuul-api-gateway-server")

**2nd Change:** We need to change the GetMapping to use CurrencyExchangeService.  
//@GetMapping(value = "currency-exchange/from/{from}/to/{to}")  
@GetMapping(value = "currency-exchange-service/currency-exchange/from/{from}/to/{to}")

Now if we call the CurrencyConversionService it will go to CurrencyExchangeService via Zuul Api Gateway  
http://localhost:8101/currency-conversion-feign/from/EUR/to/INR/quantity/1000

If we want to redirect the CurrencyConversionService via Zuul Api Gateway Server we can do like this  
http://localhost:8765/currency-conversion-service/currency-conversion-feign/from/EUR/to/INR/quantity/1000 

**Spring Sleuth:**  
Sleuth is capable of enhancing logs in many situations. Starting with version 2.0.0, Spring Cloud Sleuth 
uses Brave as the tracing library that adds unique ids to each web request that enters our application.

We need to add the spring-cloud-sleuth dependency in the required components to have each 
request a unique id and the request id will span across all components. 
Adding sleuth dependency to CurrencyConversionService, CurrencyExchangService, ZuulApiGatewayServer components.
After adding the dependencies, we need to create 
Auto Sampler bean from Brave library(which is inbuilt used by sleuth).
  
@Bean  
public Sampler defaultSampler() {  
     return Sampler.ALWAYS_SAMPLE;  
}

Now each request from CurrencyConversionService to CurrencyExchangService will pass through ZuulApiGatewayServer and 
each request will have the unique request id in each console's of the 3 components.

**Zipkin Distributed Tracing Server:**  
Zipkin is an open source project that provides mechanism for sending, receiving, storing, and visualizing traces. 
This allows us to correlate activity between servers and get a much clearer picture of exactly 
what is happening in our services.

We need to download the latest Zipkin server from https://zipkin.io/pages/quickstart.html.
After downloading we can run the server using the command **java -jar zipkin-server-2.21.5-exec.jar**.
Zipkin server is started in the port http://localhost:9411/zipkin

The Zipkin server will listen to Rabbit MQ server, so that all the trace related information's are enqueued to the RabbitMQ 
from CurrencyConversionService, CurrencyExchangService, ZuulApiGatewayServer and these traces will be read by Zipkin server.

For this we need to install RabbitMQ.   
**Step1:** Check the compatible version of RabbitMQ and Erlang in the link.  https://erlang.org/download/otp_versions_tree.html  
**Step2:** Now download and install Erlang from the link https://erlang.org/download/otp_versions_tree.html  
**Step3:** After Erlang installed, now download and install RabbitMQ from the link https://www.rabbitmq.com/download.html (Downloads on GitHub)  
**Step4:** After the installation both these Erland and RabbitMQ will be running as background process in Windows.  
**Step5:** Run rabbitmqctl.bat from the path C:\Program Files\RabbitMQ Server\rabbitmq_server-3.8.5\sbin to check the status of the RabbitMQ.  

Now RabbitMQ is ready to receive the message

We need to point the Zipkin server to read messages from RabbitMQ. For this we need to use below commands  
SET RABBIT_URI=amqp://localhost  
java -jar zipkin-server-2.21.5-exec.jar

Now the Zipkin server will listen to RabbitMQ for the incoming trace information's.

After this we need to add spring-cloud-sleuth-zipkin and spring-cloud-starter-bus-amqp to the applications
CurrencyConversionService, CurrencyExchangService, ZuulApiGatewayServer to send the trace info to the RabbitMQ.

If we restart these services and give request to CurrencyConversionService. Now we need to visit 
Zipkin dashboard http://localhost:9411/zipkin to find the traces and we can also view the dependency graph 
between these 3 components.

**In Zipkin dashboard:**  
Go to Discover menu, select serviceName from the drop down and we can see our 3 components listed in the drop down. 
Select any one of the component and click search. The result will display the list of calls happened and we can able to 
see the total traces, communication and success/failure-reasons between these 3 services and also the time taken to 
execute the services. Even we can take the trace id from the our log console and search in the Trace ID search box to 
view the combined trace among these 3 components.

**Limits Service:**   
Limits Service is connecting to SpringCloudConfigServer for fetching the property file. Where as the SpringCloudConfigServer 
is referring to CurrencyPropertyConfigRepo to fetch the property file changes based on the service name. This will be useful
for changing the property files content without the need to restart the actual applications (in this case LimitsService).  
http://localhost:8080/limits/  
http://localhost:8081/limits

Now we are trying to change the value in property file which is present in git repository CurrencyPropertyConfigRepo. 
Once property values are changed, it will reflect in SpringCloudConfigServer without any restart. We can test the 
current config by using below URL.  
http://localhost:port/{propertyname}/{profile}    
http://localhost:8888/limits-service/default

But the LimitsService which is consuming SpringCloudConfigServer, still having the old property value. 
By default property config are loaded at the application startup. Only by restarting 
the LimitsService we can able to see the updated new property value. 

But our goal is to achieve any change in property value should reflect in LimitsService without any manual restart.
So we need to add SpringBootActuator dependency to LimitsService. Now if we change a value 
in the CurrencyPropertyConfigRepo, will be reflected in LimitsService by calling the below POST method.
We need to call this POST method in all the available instances, so that all the instances will have 
now have the refreshed values without any restart.
http://localhost:8080/actuator/refresh
http://localhost:8081/actuator/refresh

There is an overhead here, Imagine if we have more than 50 instances of LimitsService we cannot call every URL manually 
to refresh the property value changes. This problem is solved by using SpringCloudBus.

**Spring Cloud Bus:**(it will either work Kafka or RabbitMQ, here we are using RabbitMQ as amqp transport protocol)  
We need to add spring-cloud-starter-bus-amqp dependency to LimitsService and SpringCloudConfigServer. Now the all 
the LimitsService instances are registered with SpringCloudBus. When there is any change in config file and if we call  
the URL http://localhost:8080/actuator/bus-refresh with POST method to refresh the config change, the 
LimitsService 8080 instance will propagate the refresh event to the SpringCloudBus and now this SpringCloudBus will 
propagate this event to all the registered LimitsService instances.

**SpringCloudBus and RabbitMQ Flow:**  
**Step1:** When calling bus-refresh URL, SpringCloudBus will get the latest config from SpringCloudConfigServer and 
update/refresh the config in the current instance of LimitsService which is running in port 8080.  
**Step2:** Then it will send a message to AMQP exchange, about information event change.  
**Step3:** All the subscribers, who are all subscribed to AMQP exchange will receive the event and make refresh to 
update the config value.

**Hystrix: Circuit Breaker**  
Hystrix helps to control the interaction between services by providing fault tolerance. It improves overall resilience 
of the system by isolating the failing services and stopping the cascading effect of failures.

A service failure in the lower level of services can cause cascading failure all the way up to the user. 
When calls to a particular service exceed **circuitBreaker.requestVolumeThreshold (default: 20 requests)** and 
the failure percentage is greater than **circuitBreaker.errorThresholdPercentage (default: >50%)** in a rolling 
window defined by **metrics.rollingStats.timeInMilliseconds (default: 10 seconds)**, the circuit opens and the 
call is not made. In cases of error and an open circuit, a fallback can be provided by the developer.

Having an open circuit stops cascading failures and allows overwhelmed or failing services time to recover. 
The fallback can be another Hystrix protected call, static data, or a sensible empty value. Fallbacks may be 
chained so that the first fallback makes some other business call, which in turn falls back to static data.

We are adding the Hystrix to CurrencyConversionService where we are depending on the CurrencyExchangeService. If the 
CurrencyExchangeService is failed or causing slowness, it will affect the entire CurrencyConversionService, so here 
we will implement Hystrix to give the fallback method during the failures of 
CurrencyExchangeService (i.e. when the circuit is open).

To enable Hystrix and Hystrix Dashboard we need to add the dependency spring-cloud-starter-netflix-hystrix and
spring-cloud-starter-netflix-hystrix-dashboard.

Now add **@EnableCircuitBreaker** and **@EnableHystrixDashboard** in the CurrencyConversionService Application class.

Also we can add the fallback method during the service call to CurrencyExchangeService, 
fallback can be mentioned using the below command  
**@HystrixCommand(fallbackMethod = "retrieveExchangeValueFallback")**  
We can give the default or static implementation instead of error response.  
http://localhost:8101/hystrix to view the dashboard and input the monitor URL as 
http://localhost:8101/hystrix.stream to view the number of success and failures calls happened between 
CurrencyConversionService and CurrencyExchangeService.


------------------------------------------------------------------------------------------------------------------------
