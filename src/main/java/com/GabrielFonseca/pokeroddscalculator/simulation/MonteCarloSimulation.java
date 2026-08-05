package com.GabrielFonseca.pokeroddscalculator.simulation;
import com.GabrielFonseca.pokeroddscalculator.model.*;
import com.GabrielFonseca.pokeroddscalculator.service.HandEvaluator;
import com.GabrielFonseca.pokeroddscalculator.service.HandResult;

import java.util.ArrayList;
import java.util.List;

public class MonteCarloSimulation {

    public OddsResult simulate(Hand playerHand, int opponents, int simulations) {

        int wins = 0;
        int losses = 0;
        int ties = 0;

        for (int i = 0; i < simulations; i++) {

            // 1. Criar um novo deck
            Deck deck = new Deck();
            deck.shuffle();

            // 2. Remover as cartas já conhecidas
            for (Card card : playerHand.getCards()) {
                deck.removeCard(card);
            }

            // 3. Criar uma cópia da mão do jogador
            Hand playerSimulationHand = new Hand();
            playerSimulationHand.addCards(playerHand.getCards());

            // 4. Criar a mão do adversário
            List<Hand> opponentsHands = new ArrayList<>();

            for (int j = 0; j < opponents; j++) {

                Hand opponent = new Hand();

                opponent.addCard(deck.draw());
                opponent.addCard(deck.draw());

                opponentsHands.add(opponent);
            }

            // 5. Completar as cartas comunitárias
            int missingCards = 7 - playerSimulationHand.size();

            for (int j = 0; j < missingCards; j++) {

                Card communityCard = deck.draw();

                playerSimulationHand.addCard(communityCard);

                for (Hand opponent : opponentsHands) {
                    opponent.addCard(communityCard);
                }
            }

            // 6. Avaliar as mãos
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

        return new OddsResult(
                wins,
                losses,
                ties,
                simulations
        );
    }
}