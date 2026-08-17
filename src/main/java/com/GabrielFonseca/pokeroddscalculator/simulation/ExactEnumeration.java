package com.GabrielFonseca.pokeroddscalculator.simulation;

import com.GabrielFonseca.pokeroddscalculator.model.*;
import com.GabrielFonseca.pokeroddscalculator.service.HandEvaluator;
import com.GabrielFonseca.pokeroddscalculator.service.HandResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Cálculo exato de equity para o river contra um único oponente.
 *
 * No river não há mais cartas por abrir: a única incógnita são as 2 cartas do
 * oponente, sorteadas entre as 45 desconhecidas. São C(45,2) = 990 possibilidades,
 * poucas o bastante para percorrer todas e obter a resposta exata — mais barato,
 * e sem a margem de erro da amostragem.
 */
public class ExactEnumeration {

    /**
     * A enumeração só compensa quando o espaço de possibilidades é pequeno.
     * Com 2 oponentes seriam C(45,2) x C(43,2) = 893.970 combinações, mais caro
     * que o Monte Carlo; com 3, o total passa de 700 milhões.
     */
    public static boolean supports(List<Card> knownCommunityCards, int opponents) {
        return knownCommunityCards.size() == 5 && opponents == 1;
    }

    public OddsResult enumerate(Hand playerHand, List<Card> knownCommunityCards) {

        if (!supports(knownCommunityCards, 1)) {
            throw new IllegalStateException(
                    "A enumeração exata cobre apenas o river contra 1 oponente"
            );
        }

        List<Card> cartasConhecidas = new ArrayList<>(playerHand.getCards());
        cartasConhecidas.addAll(knownCommunityCards);

        List<Card> cartasRestantes = cartasRestantes(cartasConhecidas);

        // No river a mão do herói está fechada: avalia uma única vez, fora do laço
        Hand maoDoHeroi = new Hand();
        maoDoHeroi.addCards(playerHand.getCards());
        maoDoHeroi.addCards(knownCommunityCards);

        HandResult resultadoDoHeroi = HandEvaluator.evaluate(maoDoHeroi);

        int wins = 0;
        int losses = 0;
        int ties = 0;

        // Cada par não ordenado de cartas restantes é uma mão possível do oponente
        for (int i = 0; i < cartasRestantes.size(); i++) {
            for (int j = i + 1; j < cartasRestantes.size(); j++) {

                Hand maoDoOponente = new Hand();
                maoDoOponente.addCard(cartasRestantes.get(i));
                maoDoOponente.addCard(cartasRestantes.get(j));
                maoDoOponente.addCards(knownCommunityCards);

                int comparacao = HandEvaluator.compare(
                        resultadoDoHeroi,
                        HandEvaluator.evaluate(maoDoOponente)
                );

                if (comparacao > 0) {
                    wins++;
                } else if (comparacao == 0) {
                    ties++;
                } else {
                    losses++;
                }
            }
        }

        return new OddsResult(wins, losses, ties, wins + losses + ties, true);
    }

    private List<Card> cartasRestantes(List<Card> cartasConhecidas) {

        List<Card> restantes = new ArrayList<>();

        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {

                Card carta = new Card(rank, suit);

                if (!cartasConhecidas.contains(carta)) {
                    restantes.add(carta);
                }
            }
        }

        return restantes;
    }
}
