package com.GabrielFonseca.pokeroddscalculator.simulation;

import com.GabrielFonseca.pokeroddscalculator.model.Card;
import com.GabrielFonseca.pokeroddscalculator.model.Hand;
import com.GabrielFonseca.pokeroddscalculator.model.OddsResult;
import com.GabrielFonseca.pokeroddscalculator.model.Rank;
import com.GabrielFonseca.pokeroddscalculator.model.Suit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExactEnumerationTest {

    private final ExactEnumeration enumeration = new ExactEnumeration();

    private static Card carta(Rank rank, Suit suit) {
        return new Card(rank, suit);
    }

    private static Hand mao(Card... cartas) {
        Hand hand = new Hand();

        for (Card carta : cartas) {
            hand.addCard(carta);
        }

        return hand;
    }

    @Test
    @DisplayName("Percorre exatamente as C(45,2) = 990 mãos possíveis do oponente")
    void percorreAsNovecentasENoventaCombinacoes() {

        Hand heroi = mao(
                carta(Rank.ACE, Suit.SPADES),
                carta(Rank.KING, Suit.DIAMONDS)
        );

        List<Card> mesa = List.of(
                carta(Rank.QUEEN, Suit.CLUBS),
                carta(Rank.JACK, Suit.HEARTS),
                carta(Rank.NINE, Suit.SPADES),
                carta(Rank.FOUR, Suit.DIAMONDS),
                carta(Rank.TWO, Suit.HEARTS)
        );

        OddsResult resultado = enumeration.enumerate(heroi, mesa);

        assertEquals(990, resultado.getSimulations());
        assertEquals(990, resultado.getWins() + resultado.getLosses() + resultado.getTies());
        assertTrue(resultado.isExact());
    }

    @Test
    @DisplayName("Royal flush imbatível vence as 990 combinações")
    void royalFlushVenceTudo() {

        // Herói fecha A-K-Q-J-10 de copas; o 10 de copas na mão dele torna
        // impossível qualquer outro royal flush, então não há vitória nem empate possível
        Hand heroi = mao(
                carta(Rank.TEN, Suit.HEARTS),
                carta(Rank.THREE, Suit.CLUBS)
        );

        List<Card> mesa = List.of(
                carta(Rank.ACE, Suit.HEARTS),
                carta(Rank.KING, Suit.HEARTS),
                carta(Rank.QUEEN, Suit.HEARTS),
                carta(Rank.JACK, Suit.HEARTS),
                carta(Rank.TWO, Suit.CLUBS)
        );

        OddsResult resultado = enumeration.enumerate(heroi, mesa);

        assertEquals(990, resultado.getWins());
        assertEquals(0, resultado.getLosses());
        assertEquals(0, resultado.getTies());
        assertEquals(100.0, resultado.getWinPercentage());
        assertEquals(100.0, resultado.getEquityPercentage(), 1e-9);
    }

    @Test
    @DisplayName("Quando a mesa é a melhor mão possível, todas as 990 combinações empatam")
    void mesaImbativelEmpataTudo() {

        // Royal flush na mesa: as cartas do herói e as do oponente são irrelevantes,
        // os dois jogam a mesa e dividem o pote em qualquer cenário
        Hand heroi = mao(
                carta(Rank.TWO, Suit.CLUBS),
                carta(Rank.THREE, Suit.DIAMONDS)
        );

        List<Card> mesa = List.of(
                carta(Rank.ACE, Suit.SPADES),
                carta(Rank.KING, Suit.SPADES),
                carta(Rank.QUEEN, Suit.SPADES),
                carta(Rank.JACK, Suit.SPADES),
                carta(Rank.TEN, Suit.SPADES)
        );

        OddsResult resultado = enumeration.enumerate(heroi, mesa);

        assertEquals(990, resultado.getTies());
        assertEquals(0, resultado.getWins());
        assertEquals(0, resultado.getLosses());
        assertEquals(50.0, resultado.getEquityPercentage(), 1e-9);
    }

    @Test
    @DisplayName("Só aceita river contra exatamente 1 oponente")
    void suportaApenasRiverContraUmOponente() {

        List<Card> river = List.of(
                carta(Rank.ACE, Suit.SPADES),
                carta(Rank.KING, Suit.SPADES),
                carta(Rank.QUEEN, Suit.SPADES),
                carta(Rank.JACK, Suit.SPADES),
                carta(Rank.TEN, Suit.SPADES)
        );

        List<Card> turn = river.subList(0, 4);
        List<Card> flop = river.subList(0, 3);

        assertTrue(ExactEnumeration.supports(river, 1));

        assertFalse(ExactEnumeration.supports(river, 2));
        assertFalse(ExactEnumeration.supports(turn, 1));
        assertFalse(ExactEnumeration.supports(flop, 1));
        assertFalse(ExactEnumeration.supports(List.of(), 1));
    }

    @Test
    @DisplayName("Recusa cenários fora do escopo em vez de devolver número errado")
    void recusaCenarioNaoSuportado() {

        Hand heroi = mao(
                carta(Rank.ACE, Suit.SPADES),
                carta(Rank.KING, Suit.DIAMONDS)
        );

        List<Card> flop = List.of(
                carta(Rank.QUEEN, Suit.CLUBS),
                carta(Rank.JACK, Suit.HEARTS),
                carta(Rank.NINE, Suit.SPADES)
        );

        assertThrows(IllegalStateException.class, () -> enumeration.enumerate(heroi, flop));
    }

    @Test
    @DisplayName("O resultado exato confere com o Monte Carlo dentro da margem amostral")
    void confereComOMonteCarlo() {

        Hand heroi = mao(
                carta(Rank.ACE, Suit.SPADES),
                carta(Rank.ACE, Suit.DIAMONDS)
        );

        List<Card> mesa = List.of(
                carta(Rank.KING, Suit.CLUBS),
                carta(Rank.SEVEN, Suit.HEARTS),
                carta(Rank.TWO, Suit.DIAMONDS),
                carta(Rank.NINE, Suit.SPADES),
                carta(Rank.THREE, Suit.CLUBS)
        );

        OddsResult exato = enumeration.enumerate(heroi, mesa);

        OddsResult estimado = new MonteCarloSimulation().simulate(heroi, mesa, 1, 30_000);

        // Com 30.000 amostras o erro padrão fica abaixo de 0,3 ponto percentual;
        // 2,5 pontos de tolerância deixam o teste estável sem perder o poder de detectar divergência real
        assertEquals(exato.getWinPercentage(), estimado.getWinPercentage(), 2.5);
        assertEquals(exato.getLossPercentage(), estimado.getLossPercentage(), 2.5);
    }
}
