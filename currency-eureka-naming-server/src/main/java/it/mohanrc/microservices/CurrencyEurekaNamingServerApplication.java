package it.mohanrc.microservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class CurrencyEurekaNamingServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CurrencyEurekaNamingServerApplication.class, args);
	}

}
