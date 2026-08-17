package com.GabrielFonseca.pokeroddscalculator.simulation;

import com.GabrielFonseca.pokeroddscalculator.model.Card;
import com.GabrielFonseca.pokeroddscalculator.model.Hand;
import com.GabrielFonseca.pokeroddscalculator.model.OddsResult;

import java.util.List;

/**
 * Escolhe a estratégia de cálculo mais adequada ao cenário.
 *
 * Quando o espaço de possibilidades é pequeno o bastante para ser percorrido por
 * inteiro, a enumeração exata é preferida: além de mais barata, devolve a resposta
 * certa em vez de uma estimativa. Nos demais casos, cai no Monte Carlo.
 */
public class OddsCalculator {

    private final MonteCarloSimulation simulation = new MonteCarloSimulation();
    private final ExactEnumeration enumeration = new ExactEnumeration();

    public OddsResult calculate(Hand playerHand, List<Card> knownCommunityCards, int opponents, int simulations) {

        if (ExactEnumeration.supports(knownCommunityCards, opponents)) {
            return enumeration.enumerate(playerHand, knownCommunityCards);
        }

        return simulation.simulate(playerHand, knownCommunityCards, opponents, simulations);
    }
}
