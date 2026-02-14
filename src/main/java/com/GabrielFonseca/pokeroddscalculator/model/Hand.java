package com.GabrielFonseca.pokeroddscalculator.model;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;


public class Hand {

    private final List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        if (cards.size() >= 7) {
            throw new IllegalStateException("Hand cannot have more than 7 cards.");
        }
        cards.add(card);
    }

    public void addCards(List<Card> newCards) {
        for (Card card : newCards) {
            addCard(card);
        }
    }

    public int size() {
        return cards.size();
    }

    public void clear() {
        cards.clear();
    }
}
