package com.GabrielFonseca.pokeroddscalculator;
import com.GabrielFonseca.pokeroddscalculator.model.*;
import com.GabrielFonseca.pokeroddscalculator.simulation.MonteCarloSimulation;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PokerOddsCalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PokerOddsCalculatorApplication.class, args);
    }

    }
