package com.GabrielFonseca.pokeroddscalculator.service;

import com.GabrielFonseca.pokeroddscalculator.model.*;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HandEvaluator {

    public HandResult evaluateFiveCards(List <Card> cards) {


        if (cards.size() < 5) {
            throw new IllegalArgumentException("Hand must have at least 5 cards.");
        }

        //
        List<Integer> values = cards.stream()
            .map(card -> card.getRank() //transforma a class Card em apenas um atributo, no caso getRank
            .getValue())
            .sorted((a,b)->a-b)
            .toList();

        boolean isFlush =   cards.stream().map(Card::getSuit).distinct().count() == 1;
        //verificar straight
        List<Integer> uniqueValues = values.stream() //coloca tudo em uma fila
                .distinct() //elimina valores iguais
                .sorted() // ordena os numeros em forma crescente
                .toList(); // coloca tudo na lista "UniqueValues"

        boolean isStraight = false;
        if(uniqueValues.size()==5){
            int min = uniqueValues.get(0);
            int max = uniqueValues.get(4);
            isStraight = (max - min == 4);// se lado direito for V isStraight tambem recebe V.
        }

        Map<Rank, Long> frequency = cards.stream().map(Card::getRank).
                collect(Collectors.groupingBy(rank->rank, Collectors.counting()));

        List <Long> counts = frequency.values().stream().sorted(Collections.reverseOrder()).toList();

        if(isFlush && isStraight){
            return new HandResult(HandRank.StraightFlush,values);
        }
        if(counts.get(0)==4){
            return new HandResult(HandRank.FourOfAKind,values);
        }
        if(counts.get(0)==3 && counts.get(1)==2){
            return new HandResult(HandRank.FullHouse,values);
        }
        if(isFlush){
            return new HandResult(HandRank.Flush,values);
        }
        if(isStraight){
            return new HandResult(HandRank.Straight,values);
        }
        if(counts.get(0)==3){
            return new HandResult(HandRank.ThreeOfAKind,values);
        }
        if(counts.get(0)==2 && counts.get(1)==2){
            return new HandResult(HandRank.TwoPair,values);
        }
        if(counts.get(0)==2){
            return new HandResult(HandRank.OnePair,values);
        }
        return new HandResult(HandRank.HighCard,values);

        // Aqui depois vamos:
        // 1. Gerar combinações de 5 cartas
        // 2. Avaliar cada uma
        // 3. Retornar a melhor


    }
}
