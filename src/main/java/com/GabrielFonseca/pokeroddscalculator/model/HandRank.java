package com.GabrielFonseca.pokeroddscalculator.model;

public enum HandRank {

    HighCard(1),
    OnePair(2),
    TwoPair(3),
    ThreeOfAKind(4),
    Straight(5),
    Flush(6),
    FullHouse(7),
    StraightFlush(8),
    FourOfAKind(9),
    royalFlush(10);

    private final int strength;

    HandRank(int strength){
        this.strength = strength;
    }

    public int getStrength(){
        return strength;
    }

}
