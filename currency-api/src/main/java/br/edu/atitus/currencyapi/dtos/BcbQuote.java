package br.edu.atitus.currencyapi.dtos;

public record BcbQuote(
        Double cotacaoCompra,
        Double cotacaoVenda,
        String dataHoraCotacao,
        String tipoBoletim
) {
}