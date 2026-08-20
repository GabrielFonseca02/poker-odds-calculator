package com.GabrielFonseca.pokeroddscalculator.simulation;

import com.GabrielFonseca.pokeroddscalculator.model.Card;
import com.GabrielFonseca.pokeroddscalculator.model.Hand;
import com.GabrielFonseca.pokeroddscalculator.model.OddsResult;
import com.GabrielFonseca.pokeroddscalculator.model.Rank;
import com.GabrielFonseca.pokeroddscalculator.model.Suit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonteCarloSimulationTest {

    private final MonteCarloSimulation simulation = new MonteCarloSimulation();

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

    /**
     * Royal flush de espadas na mesa. Nenhuma carta que qualquer jogador possa ter
     * muda o resultado: todos jogam a mesa e dividem o pote. É o que torna um
     * algoritmo aleatório determinístico e permite assertar valores exatos.
     */
    private static final List<Card> MESA_IMBATIVEL = List.of(
            carta(Rank.ACE,   Suit.SPADES),
            carta(Rank.KING,  Suit.SPADES),
            carta(Rank.QUEEN, Suit.SPADES),
            carta(Rank.JACK,  Suit.SPADES),
            carta(Rank.TEN,   Suit.SPADES)
    );

    @ParameterizedTest(name = "empate com {0} oponente(s) dá {1}% de equity")
    @CsvSource({
            "1, 50.0",
            "2, 33.333333",
            "3, 25.0",
            "9, 10.0"
    })
    @DisplayName("Empate multiway divide o pote entre todos, não sempre em dois")
    void empateMultiwayDivideEntreTodos(int oponentes, double equityEsperada) {

        Hand heroi = mao(
                carta(Rank.TWO, Suit.CLUBS),
                carta(Rank.THREE, Suit.DIAMONDS)
        );

        OddsResult resultado = simulation.simulate(heroi, MESA_IMBATIVEL, oponentes, 1_000);

        assertEquals(1_000, resultado.getTies(), "toda rodada deveria terminar empatada");
        assertEquals(0, resultado.getWins());
        assertEquals(0, resultado.getLosses());

        // A fórmula antiga (win% + tie%/2) devolveria 50% em todas as linhas
        assertEquals(equityEsperada, resultado.getEquityPercentage(), 1e-6);
    }

    @Test
    @DisplayName("Heads-up a equity continua sendo vitória + metade do empate")
    void headsUpMantemOComportamentoAntigo() {

        Hand heroi = mao(
                carta(Rank.SEVEN, Suit.DIAMONDS),
                carta(Rank.TWO, Suit.CLUBS)
        );

        OddsResult resultado = simulation.simulate(heroi, List.of(), 1, 20_000);

        // Contra 1 oponente o /2 estava certo: a correção não pode mudar este caso
        assertEquals(
                resultado.getWinPercentage() + resultado.getTiePercentage() / 2,
                resultado.getEquityPercentage(),
                1e-9
        );
    }

    @Test
    @DisplayName("Mão imbatível dá 100% de equity contra qualquer número de oponentes")
    void maoImbativelDaCemPorCento() {

        // O 10 de copas na mão do herói fecha o royal flush e, ao mesmo tempo,
        // impede que qualquer oponente forme o mesmo: não há empate possível
        Hand heroi = mao(
                carta(Rank.TEN, Suit.HEARTS),
                carta(Rank.THREE, Suit.CLUBS)
        );

        List<Card> mesa = List.of(
                carta(Rank.ACE,   Suit.HEARTS),
                carta(Rank.KING,  Suit.HEARTS),
                carta(Rank.QUEEN, Suit.HEARTS),
                carta(Rank.JACK,  Suit.HEARTS),
                carta(Rank.TWO,   Suit.CLUBS)
        );

        OddsResult resultado = simulation.simulate(heroi, mesa, 5, 1_000);

        assertEquals(1_000, resultado.getWins());
        assertEquals(0, resultado.getTies());
        assertEquals(100.0, resultado.getEquityPercentage(), 1e-9);
    }

    @Test
    @DisplayName("A equity fica sempre entre a taxa de vitória e vitória mais empate")
    void equityRespeitaOsLimites() {

        Hand heroi = mao(
                carta(Rank.ACE, Suit.SPADES),
                carta(Rank.KING, Suit.SPADES)
        );

        OddsResult resultado = simulation.simulate(heroi, List.of(), 3, 20_000);

        double equity = resultado.getEquityPercentage();

        // Um empate vale mais que uma derrota e menos que uma vitória, então a
        // equity é obrigatoriamente um ponto entre esses dois extremos. Vale para
        // qualquer entrada — é uma invariante, não um exemplo.
        assertTrue(equity >= resultado.getWinPercentage(),
                "equity " + equity + " menor que a taxa de vitória " + resultado.getWinPercentage());

        assertTrue(equity <= resultado.getWinPercentage() + resultado.getTiePercentage(),
                "equity " + equity + " maior que vitória + empate");
    }
}