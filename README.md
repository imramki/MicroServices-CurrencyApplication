Application	Port:
Limits Service						8080
Spring Cloud Config Server			8888
Currency Exchange Service			8000, 8001
Currency Conversion Service			8100, 8101
Currency Eureka Naming Server		8761
Netflix Zuul API Gateway Server		
Zipkin Distributed Tracing Server	

Spring Cloud Config Server:
sample: http://localhost:port/propertyname/default
actual: http://localhost:8888/limits-service/default

While connecting spring cloud config server from limits-service we need to add spring cloud config uri in application.properties file 
of limits-service. Access multiple properties from config server using the spring profile in the limits-service
For multiple environments:
http://localhost:8888/limits-service/dev
http://localhost:8888/limits-service/qa

Run Multiple Instances of Currency Exchange service by adding VM arguements to -Dserver.port=8001 in the run configuration

H2 database: Configured in Currecny Exchance Service
To view the console of H2 we can add the config in application.properties
spring.h2.console.enabled=true
http://localhost:8000/h2-console/

While running two instance with same H2 DB connection we will get already accessing error. For that we need to add spring.datasource.url=jdbc:h2:file:F:/MyWorks/Spring/db-h2/currency-exchange;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE
in the application.properties

RestTemplate:
Connected Exchange Service using Traditional RestTemplate Approach using the hardcoded URL

Feign:
Using open-feign removed boiler plate of RestTemplate Approach
Just we need to use @EnableFeignClients in spring boot application and 
@FeignClient with name and URL of exchange service to the newly create proxy class(below config)
@FeignClient(name = "currency-exchange-service", url = "localhost:8001")

Ribbon: Client Side Load Balancing
While implementing ribbon for load balancing we can remove the hardcoded URL in the feign proxy class
We just need to use @RibbonClient with name and add the list of servers (like bleow) to call in the application.properties
currency-exchange-service.ribbon.listOfServers=http://localhost:8000,http://localhost:8001
With config we can remove the already configured URL from @FeignClient, 
So Ribbon will distrubute load across those two server while calling the proxy from Currency Conversion Service

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

Once registered we need to extend ZuulFilter to log the request or response throught out the micro services.
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














