package it.mohanrc.microservices.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import it.mohanrc.microservices.model.ExchangeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class CurrencyExchangeService {

    @Autowired
    private RestTemplate restTemplate;

    @HystrixCommand(fallbackMethod = "retrieveExchangeValueFallback")
    public ExchangeValue retrieveExchangeValue(String from, String to) {
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("from", from);
        uriVariables.put("to", to);

        ResponseEntity<ExchangeValue> responseEntity =
                restTemplate.getForEntity("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
                        ExchangeValue.class, uriVariables);
        return responseEntity.getBody();
    }

    public ExchangeValue retrieveExchangeValueFallback(String from, String to) {
        ExchangeValue exchangeValue = new ExchangeValue();
        exchangeValue.setFrom(from);
        exchangeValue.setTo(to);
        exchangeValue.setConversionMultiple(BigDecimal.ZERO);
        return exchangeValue;
    }
}
