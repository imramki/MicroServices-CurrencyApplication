package it.mohanrc.microservices.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import it.mohanrc.microservices.model.LimitConfiguration;
import it.mohanrc.microservices.model.PropertyHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LimitsServiceController {

    @Autowired
    private PropertyHolder propertyHolder;

    @GetMapping("/limits")
    public LimitConfiguration retrieveLimts() {
        return new LimitConfiguration(propertyHolder.getMinimum(), propertyHolder.getMaximum());
    }

    @GetMapping("/limits/tolerance")
    public LimitConfiguration retrieveLimtsWithFaultTolerance() {
        return new LimitConfiguration(propertyHolder.getMinimum(), propertyHolder.getMaximum());
    }
}
