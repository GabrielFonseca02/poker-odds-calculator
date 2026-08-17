package com.GabrielFonseca.pokeroddscalculator.service;

import com.GabrielFonseca.pokeroddscalculator.model.Decision;
import com.GabrielFonseca.pokeroddscalculator.model.DecisionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DecisionAdvisorTest {

    @Test
    @DisplayName("20% de equity contra aposta de 20% do pote: paga, com EV de +8 fichas")
    void pagaQuandoOPrecoCompensa() {

        // Pote de 100, vilão aposta 20: o pote vira 120 e custa 20 para continuar
        DecisionResult resultado = DecisionAdvisor.advise(20.0, 120.0, 20.0, 1);

        assertEquals(Decision.CALL, resultado.decision());
        assertEquals(14.2857, resultado.requiredEquityPercentage(), 0.001);
        assertEquals(8.0, resultado.expectedValue(), 0.001);
    }

    @Test
    @DisplayName("A mesma mão contra aposta menor: paga, e o EV sobe para +14")
    void apostaMenorMelhoraOEv() {

        DecisionResult resultado = DecisionAdvisor.advise(20.0, 110.0, 10.0, 1);

        assertEquals(Decision.CALL, resultado.decision());
        assertEquals(8.3333, resultado.requiredEquityPercentage(), 0.001);
        assertEquals(14.0, resultado.expectedValue(), 0.001);
    }

    @Test
    @DisplayName("Desiste quando a equity não cobre o preço")
    void desisteQuandoOPrecoNaoCompensa() {

        // Precisa de 14,3% e tem 10%
        DecisionResult resultado = DecisionAdvisor.advise(10.0, 120.0, 20.0, 1);

        assertEquals(Decision.FOLD, resultado.decision());
    }

    @Test
    @DisplayName("O preço vem antes da força: aposta grande o bastante derruba até mão favorita")
    void precoPrevaleceSobreForca() {

        // 60% supera o limiar de agressão de 50%, mas a equity necessária é 83,3%
        DecisionResult resultado = DecisionAdvisor.advise(60.0, 100.0, 500.0, 1);

        assertEquals(Decision.FOLD, resultado.decision());
        assertEquals(-140.0, resultado.expectedValue(), 0.001);
    }

    @Test
    @DisplayName("Sobe quando a equity passa de 50% contra um oponente")
    void sobeComMaoFavorita() {

        DecisionResult resultado = DecisionAdvisor.advise(65.0, 120.0, 20.0, 1);

        assertEquals(Decision.RAISE, resultado.decision());
    }

    @Test
    @DisplayName("Sem aposta na frente, checa com mão fraca em vez de desistir")
    void nuncaDesisteQuandoEDeGraca() {

        DecisionResult resultado = DecisionAdvisor.advise(1.0, 100.0, 0.0, 1);

        assertEquals(Decision.CHECK, resultado.decision());
        assertEquals(0.0, resultado.requiredEquityPercentage(), 0.001);
    }

    @Test
    @DisplayName("Sem aposta na frente e com mão favorita, aposta")
    void apostaComMaoFavorita() {

        DecisionResult resultado = DecisionAdvisor.advise(70.0, 100.0, 0.0, 1);

        assertEquals(Decision.BET, resultado.decision());
    }

    @Test
    @DisplayName("O limiar de agressão cai conforme entram mais oponentes")
    void limiarDeAgressaoAcompanhaAMesa() {

        assertEquals(50.0, DecisionAdvisor.aggressionThresholdPercentage(1), 0.001);
        assertEquals(33.3333, DecisionAdvisor.aggressionThresholdPercentage(2), 0.001);
        assertEquals(25.0, DecisionAdvisor.aggressionThresholdPercentage(3), 0.001);

        // 40% é agressivo contra dois oponentes, passivo contra um
        assertEquals(Decision.BET, DecisionAdvisor.advise(40.0, 100.0, 0.0, 2).decision());
        assertEquals(Decision.CHECK, DecisionAdvisor.advise(40.0, 100.0, 0.0, 1).decision());
    }

    @Test
    @DisplayName("No ponto de equilíbrio exato, paga com EV zero")
    void pontoDeEquilibrio() {

        // Calcular o limiar em vez de escrevê-lo: 20/140 é dízima, e qualquer
        // literal truncado cai abaixo da fronteira e vira FOLD
        double limiar = DecisionAdvisor.requiredEquityPercentage(120.0, 20.0);

        DecisionResult resultado = DecisionAdvisor.advise(limiar, 120.0, 20.0, 1);

        assertEquals(Decision.CALL, resultado.decision());
        assertEquals(0.0, resultado.expectedValue(), 0.000001);
    }

    @Test
    @DisplayName("Recusa entradas inválidas")
    void recusaEntradasInvalidas() {

        assertThrows(IllegalArgumentException.class, () -> DecisionAdvisor.advise(20.0, 0.0, 10.0, 1));
        assertThrows(IllegalArgumentException.class, () -> DecisionAdvisor.advise(20.0, 100.0, -5.0, 1));
        assertThrows(IllegalArgumentException.class, () -> DecisionAdvisor.advise(150.0, 100.0, 10.0, 1));
        assertThrows(IllegalArgumentException.class, () -> DecisionAdvisor.advise(20.0, 100.0, 10.0, 0));
    }
}