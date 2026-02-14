package com.GabrielFonseca.pokeroddscalculator.model;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class Deck {
    private final List<Card> cards = new ArrayList<>();

    public Deck(){
        for(Suit suit : Suit.values()){
            for(Rank rank : Rank.values()){
                cards.add(new Card(rank, suit));
            }
        }
    }

    public List<Card> getCards(){
        return cards;
    }
    public void shuffle(){
        Collections.shuffle(cards);
    }
    public Card draw() {
        return cards.remove(0);
    }
    public void removeCard(Card card) {
        cards.remove(card);
    }


}
