package com.GabrielFonseca.pokeroddscalculator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OddsRequest(

        @Valid
        @Size(min = 2, max = 2, message = "O herói deve ter exatamente 2 cartas")
        List<CardRequest> heroCards,

        @Valid
        @Size(max = 5, message = "No máximo 5 cartas comunitárias")
        List<CardRequest> communityCards,

        @Min(value = 1, message = "Deve haver pelo menos 1 oponente")
        @Max(value = 9, message = "No máximo 9 oponentes")
        int opponents,

        @Min(value = 1000, message = "Use pelo menos 1000 simulações")
        @Max(value = 200000, message = "No máximo 200.000 simulações")
        int simulations
) {
        public OddsRequest {
                if (communityCards == null) {
                        communityCards = List.of();
                }
        }
}