package com.GabrielFonseca.pokeroddscalculator.service;


import com.GabrielFonseca.pokeroddscalculator.model.*;
import com.GabrielFonseca.pokeroddscalculator.service.HandEvaluator;
import com.GabrielFonseca.pokeroddscalculator.service.HandResult;
import com.GabrielFonseca.pokeroddscalculator.simulation.MonteCarloSimulation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HandEvaluatorTest {

    private Card card(Rank rank, Suit suit) {
        return new Card(rank, suit);
    }

    private Hand hand(Card... cards) {
        Hand hand = new Hand();
        for (Card card : cards) {
            hand.addCard(card);
        }
        return hand;
    }

    @Test
    @DisplayName("Reconhece royal flush")
    void reconheceRoyalFlush() {

        Hand hand = hand(
                card(Rank.TEN, Suit.HEARTS),
                card(Rank.JACK, Suit.HEARTS),
                card(Rank.QUEEN, Suit.HEARTS),
                card(Rank.KING, Suit.HEARTS),
                card(Rank.ACE, Suit.HEARTS)
        );

        assertEquals(HandRank.RoyalFlush, HandEvaluator.evaluate(hand).getHandRank());
    }

    @Test
    @DisplayName("Reconhece a sequência A-2-3-4-5 com o Ás valendo 1")
    void reconheceStraightComAsBaixo() {

        Hand hand = hand(
                card(Rank.ACE, Suit.HEARTS),
                card(Rank.TWO, Suit.CLUBS),
                card(Rank.THREE, Suit.DIAMONDS),
                card(Rank.FOUR, Suit.SPADES),
                card(Rank.FIVE, Suit.HEARTS)
        );

        HandResult result = HandEvaluator.evaluate(hand);

        assertEquals(HandRank.Straight, result.getHandRank());
        assertEquals(List.of(5), result.getRankingValues());
    }

    @Test
    @DisplayName("Full house ganha de flush")
    void fullHouseGanhaDeFlush() {

        Hand fullHouse = hand(
                card(Rank.KING, Suit.HEARTS),
                card(Rank.KING, Suit.CLUBS),
                card(Rank.KING, Suit.DIAMONDS),
                card(Rank.TWO, Suit.SPADES),
                card(Rank.TWO, Suit.HEARTS)
        );

        Hand flush = hand(
                card(Rank.ACE, Suit.SPADES),
                card(Rank.JACK, Suit.SPADES),
                card(Rank.NINE, Suit.SPADES),
                card(Rank.FIVE, Suit.SPADES),
                card(Rank.THREE, Suit.SPADES)
        );

        HandResult a = HandEvaluator.evaluate(fullHouse);
        HandResult b = HandEvaluator.evaluate(flush);

        assertTrue(HandEvaluator.compare(a, b) > 0);
    }

    @Test
    @DisplayName("Desempata par igual pelo kicker mais alto")
    void desempataPeloKicker() {

        Hand comKickerRei = hand(
                card(Rank.ACE, Suit.HEARTS),
                card(Rank.ACE, Suit.CLUBS),
                card(Rank.KING, Suit.DIAMONDS),
                card(Rank.SEVEN, Suit.SPADES),
                card(Rank.TWO, Suit.HEARTS)
        );

        Hand comKickerDama = hand(
                card(Rank.ACE, Suit.DIAMONDS),
                card(Rank.ACE, Suit.SPADES),
                card(Rank.QUEEN, Suit.DIAMONDS),
                card(Rank.SEVEN, Suit.HEARTS),
                card(Rank.TWO, Suit.CLUBS)
        );

        HandResult a = HandEvaluator.evaluate(comKickerRei);
        HandResult b = HandEvaluator.evaluate(comKickerDama);

        assertEquals(HandRank.OnePair, a.getHandRank());
        assertEquals(HandRank.OnePair, b.getHandRank());
        assertTrue(HandEvaluator.compare(a, b) > 0);
    }

    @Test
    @DisplayName("Com 7 cartas, escolhe a melhor combinação de 5")
    void escolheMelhorCombinacaoEntreSete() {

        Hand hand = hand(
                card(Rank.TEN, Suit.HEARTS),
                card(Rank.JACK, Suit.HEARTS),
                card(Rank.QUEEN, Suit.HEARTS),
                card(Rank.KING, Suit.HEARTS),
                card(Rank.ACE, Suit.HEARTS),
                card(Rank.TWO, Suit.CLUBS),
                card(Rank.THREE, Suit.DIAMONDS)
        );

        assertEquals(HandRank.RoyalFlush, HandEvaluator.evaluate(hand).getHandRank());
    }

    @Test
    @DisplayName("Rejeita mão com menos de 5 cartas")
    void rejeitaMaoPequena() {

        Hand hand = hand(
                card(Rank.ACE, Suit.HEARTS),
                card(Rank.KING, Suit.HEARTS)
        );

        assertThrows(IllegalArgumentException.class, () -> HandEvaluator.evaluate(hand));
    }

    @Test
    @DisplayName("A soma de vitórias, derrotas e empates é igual ao total de simulações")
    void somaBateComTotal() {

        Hand hand = new Hand();
        hand.addCard(new Card(Rank.ACE, Suit.HEARTS));
        hand.addCard(new Card(Rank.ACE, Suit.SPADES));

        OddsResult result = new MonteCarloSimulation().simulate(hand, List.of(), 3, 10_007);

        assertEquals(10_007, result.getWins() + result.getLosses() + result.getTies());
    }
}