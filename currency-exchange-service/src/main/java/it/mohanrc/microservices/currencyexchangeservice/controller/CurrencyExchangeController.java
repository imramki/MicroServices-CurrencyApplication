package it.mohanrc.microservices.currencyexchangeservice.controller;

import it.mohanrc.microservices.currencyexchangeservice.CurrencyExchangeRepository;
import it.mohanrc.microservices.currencyexchangeservice.model.ExchangeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrencyExchangeController {

    private Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Environment environment;

    @Autowired
    private CurrencyExchangeRepository currencyExchangeRepository;

    @GetMapping(value = "currency-exchange/from/{from}/to/{to}")
    public ExchangeValue retrieveExchangeValue(@PathVariable String from, @PathVariable String to) {
        ExchangeValue exchangeValue = currencyExchangeRepository.findByFromAndTo(from, to);
        String port = environment.getProperty("local.server.port");
        exchangeValue.setPort(Integer.parseInt(port));
        LOG.info("{}", exchangeValue);
        return exchangeValue;
    }
}
