package br.edu.atitus.currencyapi.dtos;

import java.util.List;

public record BcbQuoteResponse(
        List<BcbQuote> value
) {
}