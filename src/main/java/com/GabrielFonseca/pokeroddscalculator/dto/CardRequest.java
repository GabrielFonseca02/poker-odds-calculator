package com.GabrielFonseca.pokeroddscalculator.dto;

import com.GabrielFonseca.pokeroddscalculator.model.Rank;
import com.GabrielFonseca.pokeroddscalculator.model.Suit;
import jakarta.validation.constraints.NotNull;

public record CardRequest(

        @NotNull(message = "Rank é obrigatório")
        Rank rank,

        @NotNull(message = "Suit é obrigatório")
        Suit suit)
    {
}