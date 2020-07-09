Spring Cloud Config Server:
sample: http://localhost:port/propertyname/default
actual: http://localhost:8888/limits-service/default

While connecting spring cloud config server from limits-service we need to add spring cloud config uri in application.properties file 
of limits-service. Access multiple properties from config server using the spring profile in the limits-service
For multiple environments:
http://localhost:8888/limits-service/dev
http://localhost:8888/limits-service/qa

Run Multiple Instances of Currency Exchange service by adding VM arguements to -Dserver.port=8001 in the run configuration

While running two instance with same H2 DB connection we will get already accessing error. For that we need to add spring.datasource.url=jdbc:h2:~/ipinbarbot;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE
in the application.properties

