package com.GabrielFonseca.pokeroddscalculator.model;

import lombok.Getter;

@Getter
public class OddsResult {

    private final int wins;
    private final int losses;
    private final int ties;
    private final int simulations;

    /** true quando o resultado veio de enumeração exata, sem margem de erro amostral. */
    private final boolean exact;

    /**
     * Soma das frações do pote que couberam ao herói ao longo das simulações.
     *
     * Cada rodada contribui com 1 na vitória, 0 na derrota e 1/(k+1) num empate
     * com k oponentes. Guardar a soma, em vez de derivá-la de wins e ties, é o
     * que permite tratar empates multiway corretamente: o número de jogadores
     * que dividiram o pote se perde no instante em que ties é incrementado.
     */
    private final double equitySum;

    public OddsResult(int wins, int losses, int ties, int simulations, boolean exact, double equitySum) {
        this.wins = wins;
        this.losses = losses;
        this.ties = ties;
        this.simulations = simulations;
        this.exact = exact;
        this.equitySum = equitySum;
    }

    /** Fração média do pote que cabe ao herói. É o número que decide a jogada. */
    public double getEquityPercentage() {
        return (equitySum * 100.0) / simulations;
    }

    public double getWinPercentage() {
        return (wins * 100.0) / simulations;
    }

    public double getLossPercentage() {
        return (losses * 100.0) / simulations;
    }

    public double getTiePercentage() {
        return (ties * 100.0) / simulations;
    }

    @Override
    public String toString() {
        return String.format("""
                Simulações: %d
                Vitórias: %d (%.2f%%)
                Derrotas: %d (%.2f%%)
                Empates: %d (%.2f%%)
                Equity: %.2f%%
                """,
                simulations,
                wins,
                getWinPercentage(),
                losses,
                getLossPercentage(),
                ties,
                getTiePercentage(),
                getEquityPercentage());
    }
}