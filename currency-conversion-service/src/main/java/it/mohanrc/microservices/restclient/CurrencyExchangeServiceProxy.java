package it.mohanrc.microservices.restclient;

import it.mohanrc.microservices.model.ExchangeValue;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//@FeignClient(name = "currency-exchange-service", url = "localhost:8001")
//@FeignClient(name = "currency-exchange-service")
@FeignClient(name = "currency-zuul-api-gateway-server")
@RibbonClient(name = "currency-exchange-service")
public interface CurrencyExchangeServiceProxy {

    //@GetMapping(value = "currency-exchange/from/{from}/to/{to}")
    @GetMapping(value = "currency-exchange-service/currency-exchange/from/{from}/to/{to}")
    ExchangeValue retrieveExchangeValue(@PathVariable String from, @PathVariable String to);
}
