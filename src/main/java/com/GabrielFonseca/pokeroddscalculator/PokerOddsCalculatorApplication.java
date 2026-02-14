package com.GabrielFonseca.pokeroddscalculator;
import com.GabrielFonseca.pokeroddscalculator.model.Deck;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
@SpringBootApplication

public class PokerOddsCalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PokerOddsCalculatorApplication.class, args);
    }

    @Bean
    CommandLineRunner testDeck() {
        return args -> {
            Deck deck = new Deck();

            System.out.println("Total: " + deck.getCards().size());

            deck.shuffle();

            for (int i = 0; i < 5; i++) {
                System.out.println(deck.draw());
            }

            System.out.println("Restantes: " + deck.getCards().size());
        };
    }
}

