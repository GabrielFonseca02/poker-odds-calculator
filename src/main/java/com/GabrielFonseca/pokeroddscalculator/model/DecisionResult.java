package com.GabrielFonseca.pokeroddscalculator.model;

/**
 * Recomendação para uma situação de aposta.
 *
 * @param decision                       ação recomendada
 * @param equityPercentage               equity do herói, considerando empates
 * @param requiredEquityPercentage       mínimo para pagar não dar prejuízo; 0 quando não há aposta a pagar
 * @param aggressionThresholdPercentage  a partir daqui a jogada agressiva supera a passiva
 * @param expectedValue                  EV de continuar na mão (pagar, ou checar quando não há aposta), em fichas
 */
public record DecisionResult(
        Decision decision,
        double equityPercentage,
        double requiredEquityPercentage,
        double aggressionThresholdPercentage,
        double expectedValue
) {
}