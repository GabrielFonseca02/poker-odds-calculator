package com.GabrielFonseca.pokeroddscalculator.dto;

import com.GabrielFonseca.pokeroddscalculator.model.DecisionResult;
import com.GabrielFonseca.pokeroddscalculator.model.OddsResult;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Formato de saída da API.
 *
 * Existe separado do OddsResult para que o contrato público não fique amarrado
 * ao modelo de domínio: a decisão é opcional e não pertence ao resultado de uma
 * simulação, que não sabe nada sobre apostas.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OddsResponse(
        int wins,
        int losses,
        int ties,
        int simulations,
        double winPercentage,
        double lossPercentage,
        double tiePercentage,
        double equityPercentage,
        boolean exact,
        DecisionResult decision
) {

    public static OddsResponse from(OddsResult odds, DecisionResult decision) {

        return new OddsResponse(
                odds.getWins(),
                odds.getLosses(),
                odds.getTies(),
                odds.getSimulations(),
                odds.getWinPercentage(),
                odds.getLossPercentage(),
                odds.getTiePercentage(),
                odds.getEquityPercentage(),
                odds.isExact(),
                decision
        );
    }
}