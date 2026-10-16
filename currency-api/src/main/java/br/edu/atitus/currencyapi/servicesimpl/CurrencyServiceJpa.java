package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.entities.CurrencyEntity;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import br.edu.atitus.currencyapi.services.CurrencyService;
import jakarta.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CurrencyServiceJpa implements CurrencyService {

    private static final Logger logger =
            LoggerFactory.getLogger(CurrencyServiceJpa.class);

    private final CurrencyRepository repository;
    private final BcbQuoteService bcbQuoteService;

    public CurrencyServiceJpa(
            CurrencyRepository repository,
            BcbQuoteService bcbQuoteService) {
        this.repository = repository;
        this.bcbQuoteService = bcbQuoteService;
    }

    @Value("${server.port:8100}")
    private String serverPort;

    @Value("${app.bcb.quote-date:10-01-2026}")
    private String quoteDate;

    @Override
    public CurrencyResponse findBySourceCurrencyAndTargetCurrency(
            String sourceCurrency,
            String targetCurrency) throws Exception {

        String source = sourceCurrency.trim().toUpperCase(Locale.ROOT);
        String target = targetCurrency.trim().toUpperCase(Locale.ROOT);

        String environment =
                "Currency API running in Port: " + serverPort;

        String fallbackReason = "BCB sem cotação válida";

        try {
            Double rate = calculateConversionRate(source, target);

            if (isValidRate(rate)) {
                return new CurrencyResponse(
                        source,
                        target,
                        rate,
                        environment + " - Fonte: Banco Central do Brasil (PTAX)"
                );
            }

        } catch (Exception ex) {
            fallbackReason = "BCB indisponível";

            logger.warn(
                    "Falha ao consultar o BCB para {} -> {}. "
                            + "Tentando cotação local.",
                    source,
                    target,
                    ex
            );
        }

        CurrencyEntity localCurrency =
                repository.findBySourceCurrencyAndTargetCurrency(
                                source,
                                target
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Cotação não encontrada"
                                )
                        );

        return CurrencyResponse.fromEntity(
                localCurrency,
                environment + " - Fallback local (" + fallbackReason + ")"
        );
    }

    private Double calculateConversionRate(
            String source,
            String target) {

        if (source.equals(target)) {
            return 1.0;
        }

        if ("BRL".equals(source)) {
            Double targetBrlRate =
                    bcbQuoteService.getBrlSellRate(target, quoteDate);

            if (!isValidRate(targetBrlRate)) {
                return null;
            }

            return 1.0 / targetBrlRate;
        }

        Double sourceBrlRate =
                bcbQuoteService.getBrlSellRate(source, quoteDate);

        if (!isValidRate(sourceBrlRate)) {
            return null;
        }

        if ("BRL".equals(target)) {
            return sourceBrlRate;
        }

        Double targetBrlRate =
                bcbQuoteService.getBrlSellRate(target, quoteDate);

        if (!isValidRate(targetBrlRate)) {
            return null;
        }

        return sourceBrlRate / targetBrlRate;
    }

    private boolean isValidRate(Double rate) {
        return rate != null
                && Double.isFinite(rate)
                && rate > 0;
    }
}