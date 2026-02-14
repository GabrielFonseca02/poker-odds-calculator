package com.GabrielFonseca.pokeroddscalculator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@AllArgsConstructor
@Data
@ToString

public class Card {
    private Rank rank;
    private Suit suit;


}
