package it.mohanrc.microservices.currencyexchangeservice.controller;

import it.mohanrc.microservices.currencyexchangeservice.CurrencyExchangeRepository;
import it.mohanrc.microservices.currencyexchangeservice.model.ExchangeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class CurrencyExchangeController {

    @Autowired
    private Environment environment;

    @Autowired
    private CurrencyExchangeRepository currencyExchangeRepository;

    @GetMapping(value = "currency-exchange/from/{from}/to/{to}")
    public ExchangeValue retrieveExchangeValue(@PathVariable String from, @PathVariable String to) {
        ExchangeValue exchangeValue = currencyExchangeRepository.findByFromAndTo(from, to);
        String port = environment.getProperty("local.server.port");
        exchangeValue.setPort(Integer.parseInt(port));
        return exchangeValue;
    }
}
