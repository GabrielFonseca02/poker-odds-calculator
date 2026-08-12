package com.GabrielFonseca.pokeroddscalculator.controller;

import com.GabrielFonseca.pokeroddscalculator.dto.CardRequest;
import com.GabrielFonseca.pokeroddscalculator.dto.OddsRequest;
import com.GabrielFonseca.pokeroddscalculator.model.Card;
import com.GabrielFonseca.pokeroddscalculator.model.Hand;
import com.GabrielFonseca.pokeroddscalculator.model.OddsResult;
import com.GabrielFonseca.pokeroddscalculator.simulation.MonteCarloSimulation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/odds")
public class OddsController {

    private final MonteCarloSimulation simulation = new MonteCarloSimulation();
    private static final Logger logger = LoggerFactory.getLogger(OddsController.class);
    @PostMapping
    public OddsResult calculate(@Valid @RequestBody OddsRequest request) {

        Hand heroHand = new Hand();

        for (CardRequest cardRequest : request.heroCards()) {
            heroHand.addCard(new Card(cardRequest.rank(), cardRequest.suit()));
        }

        List<Card> communityCards = new ArrayList<>();

        for (CardRequest cardRequest : request.communityCards()) {
            communityCards.add(new Card(cardRequest.rank(), cardRequest.suit()));
        }

        int knownCount = communityCards.size();

        if (knownCount != 0 && knownCount != 3 && knownCount != 4 && knownCount != 5) {
            throw new IllegalArgumentException(
                    "Cartas comunitárias devem ter 0 (pré-flop), 3 (flop), 4 (turn) ou 5 (river) cartas"
            );
        }
        List<Card> todasAsCartas = new ArrayList<>();
        todasAsCartas.addAll(heroHand.getCards());
        todasAsCartas.addAll(communityCards);

        Set<Card> cartasUnicas = new HashSet<>(todasAsCartas);

        if (cartasUnicas.size() != todasAsCartas.size()) {
            throw new IllegalArgumentException("Há cartas repetidas entre sua mão e as cartas da mesa");
        }
        long inicio = System.nanoTime();

        OddsResult resultado = simulation.simulate(heroHand, communityCards, request.opponents(), request.simulations());

        long duracaoMs = (System.nanoTime() - inicio) / 1_000_000;

        logger.info("{} simulações, {} oponentes, {} cartas na mesa → {} ms",
                request.simulations(), request.opponents(), knownCount, duracaoMs);

        return resultado;
    }
}