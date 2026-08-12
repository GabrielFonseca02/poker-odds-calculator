package com.GabrielFonseca.pokeroddscalculator.simulation;
import com.GabrielFonseca.pokeroddscalculator.model.*;
import com.GabrielFonseca.pokeroddscalculator.service.HandEvaluator;
import com.GabrielFonseca.pokeroddscalculator.service.HandResult;

import java.util.ArrayList;
import java.util.List;

public class MonteCarloSimulation {


    public OddsResult simulate(Hand playerHand, List<Card> knownCommunityCards, int opponents, int simulations) {

        int nucleos = Runtime.getRuntime().availableProcessors();
        int blocos = Math.min(nucleos, simulations);

        // Divide o total em blocos de tamanho equilibrado
        List<Integer> tamanhos = new ArrayList<>();
        int base = simulations / blocos;
        int resto = simulations % blocos;

        for (int i = 0; i < blocos; i++) {
            tamanhos.add(i < resto ? base + 1 : base);
        }

        // Cada bloco roda em paralelo e devolve o SEU próprio resultado parcial
        List<OddsResult> parciais = tamanhos.parallelStream()
                .map(tamanho -> simulateChunk(playerHand, knownCommunityCards, opponents, tamanho))
                .toList();

        // Aqui já acabou tudo: uma thread só soma os parciais
        int wins = 0;
        int losses = 0;
        int ties = 0;

        for (OddsResult parcial : parciais) {
            wins += parcial.getWins();
            losses += parcial.getLosses();
            ties += parcial.getTies();
        }

        return new OddsResult(wins, losses, ties, simulations);
    }

    private OddsResult simulateChunk(Hand playerHand, List<Card> knownCommunityCards, int opponents, int simulations) {

        int wins = 0;
        int losses = 0;
        int ties = 0;

        for (int i = 0; i < simulations; i++) {

            Deck deck = new Deck();
            deck.shuffle();

            for (Card card : playerHand.getCards()) {
                deck.removeCard(card);
            }

            for (Card card : knownCommunityCards) {
                deck.removeCard(card);
            }

            Hand playerSimulationHand = new Hand();
            playerSimulationHand.addCards(playerHand.getCards());
            playerSimulationHand.addCards(knownCommunityCards);

            List<Hand> opponentsHands = new ArrayList<>();

            for (int j = 0; j < opponents; j++) {

                Hand opponent = new Hand();

                opponent.addCard(deck.draw());
                opponent.addCard(deck.draw());
                opponent.addCards(knownCommunityCards);

                opponentsHands.add(opponent);
            }

            int missingCards = 5 - knownCommunityCards.size();

            for (int j = 0; j < missingCards; j++) {

                Card communityCard = deck.draw();

                playerSimulationHand.addCard(communityCard);

                for (Hand opponent : opponentsHands) {
                    opponent.addCard(communityCard);
                }
            }

            HandResult playerResult = HandEvaluator.evaluate(playerSimulationHand);
            boolean playerWon = true;
            boolean tie = false;

            for (Hand opponent : opponentsHands) {

                HandResult opponentResult = HandEvaluator.evaluate(opponent);

                int compare = HandEvaluator.compare(playerResult, opponentResult);

                if (compare < 0) {
                    playerWon = false;
                    tie = false;
                    break;
                }

                if (compare == 0) {
                    playerWon = false;
                    tie = true;
                }
            }

            if (playerWon) {
                wins++;
            } else if (tie) {
                ties++;
            } else {
                losses++;
            }
        }

        return new OddsResult(wins, losses, ties, simulations);
    }
}