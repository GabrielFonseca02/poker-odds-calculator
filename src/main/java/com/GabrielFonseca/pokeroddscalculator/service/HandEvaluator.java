package com.GabrielFonseca.pokeroddscalculator.service;

import com.GabrielFonseca.pokeroddscalculator.model.*;
import util.CombinationGenerator;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HandEvaluator {

    public static HandResult evaluate(Hand hand) {

        if (hand.size() < 5 || hand.size() > 7) {
            throw new IllegalArgumentException(
                    "Hand must contain between 5 and 7 cards."
            );
        }

        if (hand.size() == 5) {
            return evaluateFiveCards(hand.getCards());
        }

        List<List<Card>> combinations = CombinationGenerator.generate(hand.getCards());

        HandResult bestResult = null;

        for (List<Card> combination : combinations) {

            HandResult currentResult =
                    evaluateFiveCards(combination);

            if (bestResult == null) {
                bestResult = currentResult;
            }

            else if ( compare(currentResult,bestResult)>0)
            {
                bestResult = currentResult;
            }
        }

        return bestResult;
    }

    private static HandResult evaluateFiveCards(List<Card> cards) {


        if (cards.size() < 5 || cards.size() > 7) {
            throw new IllegalArgumentException("Hand must have at least 5 cards.");
        }

        //
        List<Integer> values = cards.stream()
            .map(c -> c.getRank().getValue()) //transforma a class Card em apenas um atributo, no caso getRank
            .sorted(Collections.reverseOrder())
            .toList();

        boolean isFlush =   cards.stream().map(Card::getSuit).distinct().count() == 1;
        //verificar straight
        List<Integer> uniqueValues = values.stream() //coloca tudo em uma fila
                .distinct() //elimina valores iguais
                .sorted() // ordena os numeros em forma crescente
                .toList(); // coloca tudo na lista "UniqueValues"

        boolean isStraight = false;
        int straightHigh=0;

        if(uniqueValues.size()==5){
            int min = uniqueValues.get(0);
            int max = uniqueValues.get(4);
             if(max - min == 4){// se lado direito for V isStraight tambem recebe V.
                isStraight = true;
                 straightHigh = max;
            }

             if(uniqueValues.equals(List.of(2,3,4,5,14))){
                 isStraight = true;
                 straightHigh = 5;
             }
        }

        Map<Integer, Long> frequency = values.stream()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));
        //conta quantas vezes a chave apareceu na stream(fluxo)

        List<Map.Entry<Integer, Long>> sortedFrequency =
                frequency.entrySet().stream() //entrySet: busca a chave e o valor sem precisar acessar 2x
                        .sorted((a, b) -> {
                            int freqCompare = b.getValue().compareTo(a.getValue());
                            if (freqCompare == 0) {
                                return b.getKey().compareTo(a.getKey());
                            }
                            return freqCompare;
                        })
                        .toList();

        List<Integer> rankingValues = new ArrayList<>();

        sortedFrequency.forEach(entry -> {
            for (int i = 0; i < entry.getValue(); i++) {
                rankingValues.add(entry.getKey());
            }
        });

        // ===== CLASSIFICAÇÃO =====

        if (isStraight && isFlush) {
            if (straightHigh == 14) {
                return new HandResult(HandRank.RoyalFlush, List.of(14));
            }
            return new HandResult(HandRank.StraightFlush, List.of(straightHigh));
        }

        if (sortedFrequency.get(0).getValue() == 4) {
            return new HandResult(HandRank.FourOfAKind, rankingValues);
        }

        if (sortedFrequency.get(0).getValue() == 3 &&
                sortedFrequency.get(1).getValue() == 2) {
            return new HandResult(HandRank.FullHouse,
                    List.of(sortedFrequency.get(0).getKey(),
                            sortedFrequency.get(1).getKey()));
        }

        if (isFlush) {
            return new HandResult(HandRank.Flush, values);
        }

        if (isStraight) {
            return new HandResult(HandRank.Straight,
                    List.of(straightHigh));
        }

        if (sortedFrequency.get(0).getValue() == 3) {
            return new HandResult(HandRank.ThreeOfAKind, rankingValues);
        }

        if (sortedFrequency.get(0).getValue() == 2 &&
                sortedFrequency.get(1).getValue() == 2) {

            return new HandResult(HandRank.TwoPair,
                    List.of(
                            sortedFrequency.get(0).getKey(),
                            sortedFrequency.get(1).getKey(),
                            sortedFrequency.get(2).getKey()
                    ));
        }

        if (sortedFrequency.get(0).getValue() == 2) {
            return new HandResult(HandRank.OnePair, rankingValues);
        }

        return new HandResult(HandRank.HighCard, values);
    }

    public static int compare(HandResult a, HandResult b) {

        // 1. comparar força da mão
        int rankCompare =
                Integer.compare(
                        a.getHandRank().getStrength(),
                        b.getHandRank().getStrength()
                );

        if (rankCompare != 0) {
            return rankCompare;
        }

        // 2. comparar rankingValues
        List<Integer> aValues = a.getRankingValues();
        List<Integer> bValues = b.getRankingValues();

        for (int i = 0; i < aValues.size(); i++) {

            int valueCompare =
                    Integer.compare(
                            aValues.get(i),
                            bValues.get(i)
                    );

            if (valueCompare != 0) {
                return valueCompare;
            }
        }

        // 3. empate total
        return 0;
    }


}

