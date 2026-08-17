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

    public double getEquityPercentage() {
        return getWinPercentage() + getTiePercentage() / 2;
    }

    public OddsResult(int wins, int losses, int ties, int simulations) {
        this(wins, losses, ties, simulations, false);
    }

    public OddsResult(int wins, int losses, int ties, int simulations, boolean exact) {
        this.wins = wins;
        this.losses = losses;
        this.ties = ties;
        this.simulations = simulations;
        this.exact = exact;
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
                """,
                simulations,
                wins,
                getWinPercentage(),
                losses,
                getLossPercentage(),
                ties,
                getTiePercentage());
    }
}