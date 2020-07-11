Application	Port:
Limits Service						8080, 8081
Spring Cloud Config Server			8888
Currency Exchange Service			8000, 8001
Currency Conversion Service			8100, 8101
Currency Eureka Naming Server		8761
Netflix Zuul API Gateway Server		8765
Zipkin Distributed Tracing Server	9411

Spring Cloud Config Server:
sample: http://localhost:port/propertyname/default
actual: http://localhost:8888/limits-service/default

While connecting spring cloud config server from limits-service we need to add spring cloud config uri in application.properties 
of limits-service. Access multiple properties from config server using the spring profile in the limits-service
For multiple environments:
http://localhost:8888/limits-service/dev
http://localhost:8888/limits-service/qa

Run Multiple Instances of Currency Exchange service by adding VM arguments to -Dserver.port=8001 in the run configuration

H2 database: Configured in Currency Exchange Service
To view the console of H2 we can add the config in application.properties
spring.h2.console.enabled=true
http://localhost:8000/h2-console/

While running two instance with same H2 DB connection we will get already accessing error. For that we need to add spring.datasource.url=jdbc:h2:file:F:/MyWorks/Spring/db-h2/currency-exchange;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE
in the application.properties

RestTemplate:
Connected Exchange Service using Traditional RestTemplate Approach using the hard coded URL

Feign:
Using open-feign removed boiler plate of RestTemplate Approach
Just we need to use @EnableFeignClients in spring boot application and 
@FeignClient with name and URL of exchange service to the newly create proxy class(below config)
@FeignClient(name = "currency-exchange-service", url = "localhost:8001")

Ribbon: Client Side Load Balancing
While implementing ribbon for load balancing we can remove the hard coded URL in the feign proxy class
We just need to use @RibbonClient with name and add the list of servers (like below) to call in the application.properties
currency-exchange-service.ribbon.listOfServers=http://localhost:8000,http://localhost:8001
With config we can remove the already configured URL from @FeignClient, 
So Ribbon will distribute load across those two server while calling the proxy from Currency Conversion Service

//@FeignClient(name = "currency-exchange-service", url = "localhost:8001")
@FeignClient(name = "currency-exchange-service")
@RibbonClient(name = "currency-exchange-service")

Eureka Naming Server: Service Registration and Service Discovery
We need to create new component with @EnableEurekaServer in spring boot application class
by default Eureka port is 8761. Once the application is running we can view the Eureka dashboard in http://localhost:8761/
Also we can stop registering this service in Eureka by adding the below config in application.properties
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false

Eureka Client:
We can add the annotation @EnableDiscoverClient in each of the spring boot applications of currency application,
which will by default register all the instances of applications to the Eureka server. If we want to configure the 
Eureka Server URL explicitly we can configure in application.properties like below.
eureka.client.service-url.default-zone=http://localhost:8761

LimitsService, CurrencyExchangService, CurrencyConversionService all registered with Eureka Server by doing above steps.

Now we can remove the listOfServers config for the Ribbon we did in Currency Conversion Service, because Ribbon will 
use the Eureka server to find out the list of instances available to load balance. Ribbon will use the name we registered 
in the @RibbonClient annotation and refer with Eureka server to fetch the list of instances. 
(commented the config from application.properties)
#currency-exchange-service.ribbon.listOfServers=http://localhost:8000,http://localhost:8001

Zuul: Api Gateway
Api Gateway is used to implement common functionality across all the micro services.
1) Authentication, Authorization and Security
2) Logging
3) Aggregating group of services

New component is created as currency-zuul-api-gateway-server with zuul as dependency
Add @EnableZuulProxy to the spring boot application class, and also register this service class with naming server to
make a request using the service name.

Once registered we need to extend ZuulFilter to log the request or response through out the micro services.
CurrencyExchangService URL will be http://localhost:8001/currency-exchange/from/USD/to/INR
When we redirect via Zuul we need to call by using the service name of CurrencyExchangService.
http://localhost:8765/{application-name}/{URI}
http://localhost:8765/currency-exchange-service/currency-exchange/from/USD/to/INR
Now the currency exchange request will be logged in Zuul Api Gateway component, likewise we can implement 
security for all other services in Zuul Api Gateway server component.
 
If we want to consume the CurrencyExchangService from CurrencyConversionService we need to do the following
changes to the CurrencyExchangeServiceProxy we created early in CurrencyConversionService.
1st Change: We need to point to zuul api gateway in FeignClient
//@FeignClient(name = "currency-exchange-service")
@FeignClient(name = "currency-zuul-api-gateway-server")

2nd Change: We need to change the GetMapping to use currency-exchange-service.
//@GetMapping(value = "currency-exchange/from/{from}/to/{to}")
@GetMapping(value = "currency-exchange-service/currency-exchange/from/{from}/to/{to}")

Now if we call the CurrencyConversionService it will go through CurrencyExchangService via Zuul Api Gateway
http://localhost:8101/currency-conversion-feign/from/EUR/to/INR/quantity/1000

If we want to redirect the CurrencyConversionService to Zuul Api Gateway Server we can do like this
http://localhost:8765/currency-conversion-service/currency-conversion-feign/from/EUR/to/INR/quantity/1000 

Spring Sleuth:
Sleuth is capable of enhancing logs in many situations. Starting with version 2.0.0, Spring Cloud Sleuth 
uses Brave as the tracing library that adds unique ids to each web request that enters our application.

We need to add the spring-cloud-sleuth dependency in the required services to assign each 
request a unique id across all the components. 
Added sleuth dependency to CurrencyConversionService, CurrencyExchangService, ZuulApiGatewayServer components
After adding the dependency, we need to create 
Auto Sampler bean from Brave library(which is inbuilt used by sleuth).
@Bean
public Sampler defaultSampler() {
	return Sampler.ALWAYS_SAMPLE;
}

Now each request from CurrencyConversionService=>CurrencyExchangService via ZuulApiGatewayServer will have 
the same unique request id for each individual request across 3 differnent consoles.

Zipkin Distributed Tracing Server:
Zipkin is an open source project that provides mechanisms for sending, receiving, storing, and visualizing traces. 
This allows us to correlate activity between servers and get a much clearer picture of exactly 
what is happening in our services.

We need to download the latest Zipkin server from https://zipkin.io/pages/quickstart.html.
After downloading we can run the server using java -jar zipkin-server-2.21.5-exec.jar command
Now the Zipkin server is started in the port http://localhost:9411/zipkin

The Zipkin server will listen to Rabbit MQ server, so that all the traces enqueued to the RabbitMQ 
by CurrencyConversionService, CurrencyExchangService, ZuulApiGatewayServer will be read by Zipkin server.

For this we need to install RabbitMQ. 
Step1: Check the compatible version of RabbitMQ and Erlang OTP using https://erlang.org/download/otp_versions_tree.html
Step2: Now download and install Erlang OTP from https://erlang.org/download/otp_versions_tree.html
Step3: After Erlang OTP installed, now download and install from https://www.rabbitmq.com/download.html (Downloads on GitHub)
Step4: Check rabbitmqctl.bat status in the path C:\Program Files\RabbitMQ Server\rabbitmq_server-3.8.5\sbin
Step5: Now RabbitMQ is ready to receive the message

We need to point Zipkin server to read from RabbitMQ. For this we need to below commands
SET RABBIT_URI=amqp://localhost
java -jar zipkin-server-2.21.5-exec.jar

Now the Zipkin will listen to RabbitMQ for the incoming message.

After this we need to add spring-cloud-sleuth-zipkin and spring-cloud-starter-bus-amqp to the below applications
CurrencyConversionService, CurrencyExchangService, ZuulApiGatewayServer

If we restart these services and give request to CurrencyConversionService the response will come as expected. Now 
we need to visit http://localhost:9411/zipkin to find the traces and view the dependency between these 3 services.

In Zipkin dashboard:
Go to Discover menu, select serviceName from the drop down and we can see our 3 services in the drop down. 
Select any of our services and click search. The result will display the list of calls happened with the given 
serviceName and we can able to see the total traces, communication and success/failure-reasons between these 3 services
and the timing to take to execute the services will also be present. Even we can take the trace id from the our log 
console and search in the Trace ID search box to view the combined trace among these 3 services.

Limits Service: 
Limits Service is connecting to SpringCloudConfigServer for fetching the property file. Where as the SpringCloudConfigServer 
is referring to CurrencyPropertyConfigRepo to fetch the property file changes based on the service name. This will be useful
for changing the property files content without restarting the actual application (in this case Limits Service).
http://localhost:8080/limits
http://localhost:8081/limits

Now we are trying to change the value in property file which is present in git repository CurrencyPropertyConfigRepo. 
Once property changed it will reflect in SpringCloudConfigServer without any restart of the component by using below URL.
http://localhost:8888/limits-service/default

But the LimitsService which is consuming SpringCloudConfigServer, still having the old property value. Only by restarting 
this LimitsService will help us to update the new property. 

But our goal is to achieve any change in property file should reflect in LimitsService without any restart.
So we need to add SpringBootActuator dependency to LimitsService and restart to take effect. Now if we change a value 
in the CurrencyPropertyConfigRepo, will be reflected in LimitsService by calling this POST method.
We need to call this POST method in both instances and now the values are refreshed in LimitsService without any restart.
http://localhost:8080/actuator/refresh
http://localhost:8081/actuator/refresh

If we have more than 10 instances we cannot call every URL manually to refresh the property file change. This problem is 
solved by Spring Cloud Bus.

Spring Cloud Bus: (it will either Kafka or RabbitMQ, here we are using RabbitMQ as amqp transport protocol)
We need to add spring-cloud-starter-bus-amqp dependency to LimitsService and SpringCloudConfigServer. Now the all 
the LimitsService instances are registered with SpringCloudBus. When there is any change in config file and if we call  
the URL http://localhost:8080/actuator/bus-refresh with POST method to refresh the config change, the 
LimitsService 8080 instance will propagate the event to SpringCloudBus, now this SpringCloudBus will propagate 
the event to all the registered instances in it.

SpringCloudBus and RabbitMQ Flow:
Step1: When calling bus-refresh URL, SpringCloudBus will get the latest config from SpringCloudConfigServer and update 
the current instance of LimitsService which is running in port 8080.
Step2: Then send a message to AMQP exchange about information event change.
Step3: All the subscribers to who are all subscribed to AMQP exchange will receive the event and make refresh to 
update their config's.
