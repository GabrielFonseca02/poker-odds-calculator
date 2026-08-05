package com.GabrielFonseca.pokeroddscalculator.service;

import com.GabrielFonseca.pokeroddscalculator.model.HandRank;
import lombok.Getter;

import java.util.List;

@Getter
public class HandResult {

    private final HandRank handRank;

    private final List<Integer> rankingValues;

    public HandResult(
            HandRank handRank,
            List<Integer> rankingValues
    ) {
        this.handRank = handRank;
        this.rankingValues = rankingValues;
    }

    @Override
    public String toString() {
        return handRank +
                " " +
                rankingValues;
    }
}