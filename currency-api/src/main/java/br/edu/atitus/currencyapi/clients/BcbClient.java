package br.edu.atitus.currencyapi.clients;

import br.edu.atitus.currencyapi.dtos.BcbQuoteResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface BcbClient {

    @GetExchange(
            "/olinda/servico/PTAX/versao/v1/odata/" +
                    "CotacaoMoedaDia(moeda=@moeda,dataCotacao=@dataCotacao)"
    )
    BcbQuoteResponse getQuote(
            @RequestParam("@moeda") String currency,
            @RequestParam("@dataCotacao") String quoteDate,
            @RequestParam("$format") String format
    );
}