package it.mohanrc.microservices.controller;

import it.mohanrc.microservices.model.CurrencyConversion;
import it.mohanrc.microservices.model.ExchangeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
public class CurrencyConversionController {

    @Autowired
    private Environment environment;

    @GetMapping(value = "currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion retrieveExchangeValue(@PathVariable String from, @PathVariable String to,
                                                    @PathVariable BigDecimal quantity) {
        String port = environment.getProperty("local.server.port");
        ExchangeValue exchangeValue = callCurrencyExchangeService(from, to);
        CurrencyConversion currencyConversion =
                new CurrencyConversion(exchangeValue.getId(), from, to,
                        exchangeValue.getConversionMultiple(), quantity, quantity.multiply(exchangeValue.getConversionMultiple()));
        currencyConversion.setPort(Integer.parseInt(port));
        return currencyConversion;
    }

    private ExchangeValue callCurrencyExchangeService(String from, String to) {
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("from", from);
        uriVariables.put("to", to);

        ResponseEntity<ExchangeValue> responseEntity =
                new RestTemplate()
                        .getForEntity("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
                                ExchangeValue.class, uriVariables);
        return responseEntity.getBody();
    }
}