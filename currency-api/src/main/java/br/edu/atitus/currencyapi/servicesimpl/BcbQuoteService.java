package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.clients.BcbClient;
import br.edu.atitus.currencyapi.dtos.BcbQuote;
import br.edu.atitus.currencyapi.dtos.BcbQuoteResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Locale;

@Service
public class BcbQuoteService {

    private final BcbClient bcbClient;

    public BcbQuoteService(BcbClient bcbClient) {
        this.bcbClient = bcbClient;
    }

    @Cacheable(
            cacheNames = "bcb-currency",
            key = "#currency + '-' + #quoteDate",
            unless = "#result == null"
    )
    @Retry(name = "bcb-api")
    @CircuitBreaker(name = "bcb-api")
    public Double getBrlSellRate(String currency, String quoteDate) {

        String normalizedCurrency =
                currency.trim().toUpperCase(Locale.ROOT);

        BcbQuoteResponse response = bcbClient.getQuote(
                "'" + normalizedCurrency + "'",
                "'" + quoteDate + "'",
                "json"
        );

        if (response == null
                || response.value() == null
                || response.value().isEmpty()) {
            return null;
        }

        return response.value().stream()
                .filter(quote -> quote.cotacaoVenda() != null
                        && quote.cotacaoVenda() > 0)
                .max(Comparator.comparing(
                        quote -> quote.dataHoraCotacao() == null
                                ? ""
                                : quote.dataHoraCotacao()
                ))
                .map(BcbQuote::cotacaoVenda)
                .orElse(null);
    }
}