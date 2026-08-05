package com.GabrielFonseca.pokeroddscalculator.controller;
import jakarta.validation.Valid;
import com.GabrielFonseca.pokeroddscalculator.dto.CardRequest;
import com.GabrielFonseca.pokeroddscalculator.dto.OddsRequest;
import com.GabrielFonseca.pokeroddscalculator.model.Card;
import com.GabrielFonseca.pokeroddscalculator.model.Hand;
import com.GabrielFonseca.pokeroddscalculator.model.OddsResult;
import com.GabrielFonseca.pokeroddscalculator.simulation.MonteCarloSimulation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/odds")
public class OddsController {

    private final MonteCarloSimulation simulation = new MonteCarloSimulation();

    @PostMapping
    public OddsResult calculate(@Valid @RequestBody OddsRequest request) {

        Hand heroHand = new Hand();

        for (CardRequest cardRequest : request.heroCards()) {
            heroHand.addCard(new Card(cardRequest.rank(), cardRequest.suit()));
        }

        return simulation.simulate(heroHand, request.opponents(), request.simulations());
    }
}