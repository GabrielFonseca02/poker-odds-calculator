package com.GabrielFonseca.pokeroddscalculator.service;

import com.GabrielFonseca.pokeroddscalculator.model.Decision;
import com.GabrielFonseca.pokeroddscalculator.model.DecisionResult;

/**
 * Traduz equity e tamanho do pote em uma recomendação de jogada.
 */
public class DecisionAdvisor {

    public static DecisionResult advise(double equityPercentage, double pot, double toCall, int opponents) {

        if (pot <= 0) {
            throw new IllegalArgumentException("O pote deve ser maior que zero");
        }

        if (toCall < 0) {
            throw new IllegalArgumentException("O valor a pagar não pode ser negativo");
        }

        if (equityPercentage < 0 || equityPercentage > 100) {
            throw new IllegalArgumentException("A equity deve estar entre 0 e 100");
        }

        if (opponents < 1) {
            throw new IllegalArgumentException("Deve haver pelo menos 1 oponente");
        }

        double requiredEquity = requiredEquityPercentage(pot, toCall);
        double aggressionThreshold = aggressionThresholdPercentage(opponents);
        double expectedValue = expectedValueOfContinuing(equityPercentage, pot, toCall);

        Decision decision = decide(equityPercentage, requiredEquity, aggressionThreshold, toCall);

        return new DecisionResult(
                decision,
                equityPercentage,
                requiredEquity,
                aggressionThreshold,
                expectedValue
        );
    }

    private static Decision decide(double equityPercentage,
                                   double requiredEquityPercentage,
                                   double aggressionThresholdPercentage,
                                   double toCall) {

        // Sem aposta na frente, continuar não custa nada: desistir nunca é a melhor jogada
        if (toCall == 0) {
            return equityPercentage > aggressionThresholdPercentage
                    ? Decision.BET
                    : Decision.CHECK;
        }

        // O preço vem antes da força da mão: por pior que seja o desconto,
        // uma aposta grande o bastante torna qualquer mão impagável
        if (equityPercentage < requiredEquityPercentage) {
            return Decision.FOLD;
        }

        return equityPercentage > aggressionThresholdPercentage
                ? Decision.RAISE
                : Decision.CALL;
    }

    /** Ponto de equilíbrio: abaixo desta equity, pagar dá prejuízo no longo prazo. */
    static double requiredEquityPercentage(double pot, double toCall) {

        if (toCall == 0) {
            return 0;
        }

        return (toCall * 100.0) / (pot + toCall);
    }

    /**
     * Acima desta equity, a jogada agressiva rende mais que a passiva.
     *
     * Assumindo que todos paguem, a diferença entre apostar e checar se reduz a
     * B x (e x (n + 1) - 1), cujo sinal depende apenas de e > 1 / (n + 1).
     * Contra 1 oponente dá 50%; contra 2, 33,3%; contra 3, 25%.
     */
    static double aggressionThresholdPercentage(int opponents) {
        return 100.0 / (opponents + 1);
    }

    /** EV de continuar na mão, em fichas. Positivo significa lucro médio no longo prazo. */
    static double expectedValueOfContinuing(double equityPercentage, double pot, double toCall) {

        double equity = equityPercentage / 100.0;

        return equity * pot - (1 - equity) * toCall;
    }
}