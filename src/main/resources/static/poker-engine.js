/**
 * Motor de cálculo de odds — porte para JavaScript da lógica de domínio.
 *
 * Espelha HandEvaluator, ExactEnumeration, MonteCarloSimulation e DecisionAdvisor,
 * mantendo os mesmos critérios de classificação e desempate. A diferença está na
 * representação: em vez de objetos Card, cada carta é um inteiro de 0 a 51, e cada
 * mão avaliada vira um único número. Isso elimina a alocação dentro do laço de
 * simulação, que é o que permite rodar 200.000 simulações no navegador.
 *
 * Carregado tanto pela página quanto pelos workers, por isso não toca no DOM.
 */
(function (global) {
    'use strict';

    // ===== CARTAS =====
    //
    // índice = naipe * 13 + (valor - 2), na mesma ordem em que o Deck do back-end
    // preenche o baralho. Valor e naipe saem de volta por aritmética.

    const RANK_VALUE = {
        TWO: 2, THREE: 3, FOUR: 4, FIVE: 5, SIX: 6, SEVEN: 7, EIGHT: 8,
        NINE: 9, TEN: 10, JACK: 11, QUEEN: 12, KING: 13, ACE: 14
    };

    const SUIT_INDEX = { HEARTS: 0, DIAMONDS: 1, CLUBS: 2, SPADES: 3 };

    function indiceDaCarta(carta) {
        const valor = RANK_VALUE[carta.rank];
        const naipe = SUIT_INDEX[carta.suit];

        if (valor === undefined || naipe === undefined) {
            throw new RangeError('Carta inválida');
        }

        return naipe * 13 + (valor - 2);
    }

    // ===== COMBINAÇÕES =====
    //
    // Equivale ao CombinationGenerator, mas gerado uma única vez no carregamento e
    // guardado como índices de posição, não como cartas. C(7,5) = 21 combinações.

    function tabelaDeCombinacoes(total) {
        const combinacoes = [];
        const atual = [];

        (function gerar(inicio) {
            if (atual.length === 5) {
                combinacoes.push(atual.slice());
                return;
            }

            for (let i = inicio; i < total; i++) {
                atual.push(i);
                gerar(i + 1);
                atual.pop();
            }
        })(0);

        return Int8Array.from([].concat.apply([], combinacoes));
    }

    const COMBINACOES = {
        5: tabelaDeCombinacoes(5),
        6: tabelaDeCombinacoes(6),
        7: tabelaDeCombinacoes(7)
    };

    // ===== AVALIAÇÃO DE MÃO =====
    //
    // O HandResult do back-end (categoria + lista de desempate) vira um único inteiro:
    //
    //     categoria * 16^5 + d0 * 16^4 + d1 * 16^3 + d2 * 16^2 + d3 * 16 + d4
    //
    // Como nenhum valor passa de 14 e cada categoria sempre produz uma lista de
    // desempate do mesmo tamanho, comparar dois inteiros dá exatamente a mesma ordem
    // que o HandEvaluator.compare percorrendo as listas posição a posição.

    const CATEGORIA = {
        HIGH_CARD: 1, ONE_PAIR: 2, TWO_PAIR: 3, THREE_OF_A_KIND: 4, STRAIGHT: 5,
        FLUSH: 6, FULL_HOUSE: 7, FOUR_OF_A_KIND: 8, STRAIGHT_FLUSH: 9, ROYAL_FLUSH: 10
    };

    const PESO_CATEGORIA = 1048576; // 16^5
    const PESO_PRIMEIRO = 65536;    // 16^4

    function pontuarUm(categoria, a) {
        return categoria * PESO_CATEGORIA + a * 65536;
    }

    function pontuarDois(categoria, a, b) {
        return categoria * PESO_CATEGORIA + a * 65536 + b * 4096;
    }

    function pontuarTres(categoria, a, b, c) {
        return categoria * PESO_CATEGORIA + a * 65536 + b * 4096 + c * 256;
    }

    function pontuarLista(categoria, desempate, tamanho) {
        let pontos = categoria * PESO_CATEGORIA;
        let peso = PESO_PRIMEIRO;

        for (let i = 0; i < tamanho; i++) {
            pontos += desempate[i] * peso;
            peso /= 16;
        }

        return pontos;
    }

    // Reaproveitados a cada avaliação: dentro do laço de simulação, alocar custaria caro
    const _ocorrencias = new Int8Array(15);
    const _valorPorFreq = new Int8Array(5);
    const _contagemPorFreq = new Int8Array(5);
    const _decrescente = new Int8Array(5);
    const _agrupado = new Int8Array(5);

    /** Avalia exatamente 5 cartas e devolve a pontuação da mão. */
    function avaliarCinco(c0, c1, c2, c3, c4) {

        _ocorrencias.fill(0);
        _ocorrencias[(c0 % 13) + 2]++;
        _ocorrencias[(c1 % 13) + 2]++;
        _ocorrencias[(c2 % 13) + 2]++;
        _ocorrencias[(c3 % 13) + 2]++;
        _ocorrencias[(c4 % 13) + 2]++;

        const naipe = (c0 / 13) | 0;

        const isFlush =
            ((c1 / 13) | 0) === naipe &&
            ((c2 / 13) | 0) === naipe &&
            ((c3 / 13) | 0) === naipe &&
            ((c4 / 13) | 0) === naipe;

        // Percorrendo de 14 para 2, os valores já saem em ordem decrescente
        let distintos = 0;
        let quantosNaLista = 0;
        let maior = 0;
        let menor = 0;

        for (let valor = 14; valor >= 2; valor--) {
            const vezes = _ocorrencias[valor];

            if (vezes === 0) {
                continue;
            }

            if (maior === 0) {
                maior = valor;
            }
            menor = valor;

            _valorPorFreq[distintos] = valor;
            _contagemPorFreq[distintos] = vezes;
            distintos++;

            for (let i = 0; i < vezes; i++) {
                _decrescente[quantosNaLista++] = valor;
            }
        }

        // ===== SEQUÊNCIA =====

        let isStraight = false;
        let topoDaSequencia = 0;

        if (distintos === 5) {

            if (maior - menor === 4) {
                isStraight = true;
                topoDaSequencia = maior;
            }

            // A-2-3-4-5: aqui o Ás vale 1 e a sequência termina no 5
            if (_ocorrencias[14] && _ocorrencias[2] && _ocorrencias[3] &&
                _ocorrencias[4] && _ocorrencias[5]) {
                isStraight = true;
                topoDaSequencia = 5;
            }
        }

        // Ordena por frequência decrescente. A inserção só desloca quando a contagem é
        // estritamente maior, então valores de mesma frequência preservam a ordem
        // decrescente em que foram encontrados — o mesmo critério do comparador do back-end.
        for (let i = 1; i < distintos; i++) {
            const valor = _valorPorFreq[i];
            const vezes = _contagemPorFreq[i];
            let j = i - 1;

            while (j >= 0 && _contagemPorFreq[j] < vezes) {
                _valorPorFreq[j + 1] = _valorPorFreq[j];
                _contagemPorFreq[j + 1] = _contagemPorFreq[j];
                j--;
            }

            _valorPorFreq[j + 1] = valor;
            _contagemPorFreq[j + 1] = vezes;
        }

        let quantosAgrupados = 0;

        for (let i = 0; i < distintos; i++) {
            for (let k = 0; k < _contagemPorFreq[i]; k++) {
                _agrupado[quantosAgrupados++] = _valorPorFreq[i];
            }
        }

        // ===== CLASSIFICAÇÃO =====

        if (isStraight && isFlush) {
            return topoDaSequencia === 14
                ? pontuarUm(CATEGORIA.ROYAL_FLUSH, 14)
                : pontuarUm(CATEGORIA.STRAIGHT_FLUSH, topoDaSequencia);
        }

        if (_contagemPorFreq[0] === 4) {
            return pontuarLista(CATEGORIA.FOUR_OF_A_KIND, _agrupado, quantosAgrupados);
        }

        if (_contagemPorFreq[0] === 3 && _contagemPorFreq[1] === 2) {
            return pontuarDois(CATEGORIA.FULL_HOUSE, _valorPorFreq[0], _valorPorFreq[1]);
        }

        if (isFlush) {
            return pontuarLista(CATEGORIA.FLUSH, _decrescente, quantosNaLista);
        }

        if (isStraight) {
            return pontuarUm(CATEGORIA.STRAIGHT, topoDaSequencia);
        }

        if (_contagemPorFreq[0] === 3) {
            return pontuarLista(CATEGORIA.THREE_OF_A_KIND, _agrupado, quantosAgrupados);
        }

        if (_contagemPorFreq[0] === 2 && _contagemPorFreq[1] === 2) {
            return pontuarTres(CATEGORIA.TWO_PAIR,
                _valorPorFreq[0], _valorPorFreq[1], _valorPorFreq[2]);
        }

        if (_contagemPorFreq[0] === 2) {
            return pontuarLista(CATEGORIA.ONE_PAIR, _agrupado, quantosAgrupados);
        }

        return pontuarLista(CATEGORIA.HIGH_CARD, _decrescente, quantosNaLista);
    }

    /** Melhor mão de 5 cartas dentro de 5, 6 ou 7 — equivale ao HandEvaluator.evaluate. */
    function avaliarMelhor(cartas, quantidade) {

        if (quantidade < 5 || quantidade > 7) {
            throw new RangeError('A mão deve conter entre 5 e 7 cartas');
        }

        if (quantidade === 5) {
            return avaliarCinco(cartas[0], cartas[1], cartas[2], cartas[3], cartas[4]);
        }

        const combinacoes = COMBINACOES[quantidade];
        let melhor = -1;

        for (let i = 0; i < combinacoes.length; i += 5) {
            const pontos = avaliarCinco(
                cartas[combinacoes[i]],
                cartas[combinacoes[i + 1]],
                cartas[combinacoes[i + 2]],
                cartas[combinacoes[i + 3]],
                cartas[combinacoes[i + 4]]
            );

            if (pontos > melhor) {
                melhor = pontos;
            }
        }

        return melhor;
    }

    // ===== BARALHO RESTANTE =====

    function cartasRestantes(conhecidas) {
        const restantes = new Int8Array(52 - conhecidas.length);
        let quantas = 0;

        for (let carta = 0; carta < 52; carta++) {
            if (conhecidas.indexOf(carta) === -1) {
                restantes[quantas++] = carta;
            }
        }

        return restantes;
    }

    // ===== ENUMERAÇÃO EXATA =====
    //
    // No river contra 1 oponente não há mais cartas por abrir: a única incógnita são
    // as 2 cartas dele, sorteadas entre as 45 desconhecidas. São C(45,2) = 990
    // possibilidades, poucas o bastante para percorrer todas e obter a resposta certa
    // em vez de uma estimativa.

    function suportaEnumeracao(cartasDaMesa, oponentes) {
        return cartasDaMesa.length === 5 && oponentes === 1;
    }

    function enumerar(mao, cartasDaMesa) {

        const restantes = cartasRestantes(mao.concat(cartasDaMesa));

        const cartasDoHeroi = new Int8Array(7);
        cartasDoHeroi[0] = mao[0];
        cartasDoHeroi[1] = mao[1];

        for (let i = 0; i < 5; i++) {
            cartasDoHeroi[2 + i] = cartasDaMesa[i];
        }

        // No river a mão do herói está fechada: avalia uma vez só, fora do laço
        const pontosDoHeroi = avaliarMelhor(cartasDoHeroi, 7);

        const cartasDoOponente = new Int8Array(7);

        for (let i = 2; i < 7; i++) {
            cartasDoOponente[i] = cartasDoHeroi[i];
        }

        let wins = 0;
        let losses = 0;
        let ties = 0;

        // A enumeração só cobre heads-up, então todo empate é a dois: 1/2 para cada um.
        // O valor continua explícito em vez de assumido, para não ressuscitar o /2
        // caso um dia isto passe a suportar mais oponentes.
        let equitySum = 0.0;

        // Cada par não ordenado de cartas restantes é uma mão possível do oponente
        for (let i = 0; i < restantes.length; i++) {
            for (let j = i + 1; j < restantes.length; j++) {

                cartasDoOponente[0] = restantes[i];
                cartasDoOponente[1] = restantes[j];

                const comparacao = pontosDoHeroi - avaliarMelhor(cartasDoOponente, 7);

                if (comparacao > 0) {
                    wins++;
                    equitySum += 1.0;
                } else if (comparacao === 0) {
                    ties++;
                    equitySum += 0.5;
                } else {
                    losses++;
                }
            }
        }

        return {
            wins: wins,
            losses: losses,
            ties: ties,
            simulations: wins + losses + ties,
            equitySum: equitySum
        };
    }

    // ===== MONTE CARLO =====

    /**
     * Roda um bloco de simulações. O runner divide o total entre os núcleos disponíveis
     * e soma os parciais, do mesmo jeito que o parallelStream faz no back-end.
     */
    function simularBloco(mao, cartasDaMesa, oponentes, simulacoes) {

        const restantes = cartasRestantes(mao.concat(cartasDaMesa));
        const total = restantes.length;

        const abertas = cartasDaMesa.length;
        const faltamAbrir = 5 - abertas;
        const necessarias = oponentes * 2 + faltamAbrir;

        const cartasDoHeroi = new Int8Array(7);
        cartasDoHeroi[0] = mao[0];
        cartasDoHeroi[1] = mao[1];

        for (let i = 0; i < abertas; i++) {
            cartasDoHeroi[2 + i] = cartasDaMesa[i];
        }

        const cartasDoOponente = new Int8Array(7);

        let wins = 0;
        let losses = 0;
        let ties = 0;
        let equitySum = 0.0;

        for (let n = 0; n < simulacoes; n++) {

            // Fisher-Yates parcial: embaralhar as 52 a cada rodada seria desperdício,
            // basta sortear as poucas cartas que a rodada vai realmente consumir
            for (let i = 0; i < necessarias; i++) {
                const j = i + ((Math.random() * (total - i)) | 0);
                const troca = restantes[i];
                restantes[i] = restantes[j];
                restantes[j] = troca;
            }

            // Os oponentes compram primeiro, depois abre o que faltava da mesa —
            // a ordem não muda a probabilidade, mas mantém o paralelo com o back-end
            for (let i = 0; i < faltamAbrir; i++) {
                cartasDoHeroi[2 + abertas + i] = restantes[oponentes * 2 + i];
            }

            const pontosDoHeroi = avaliarMelhor(cartasDoHeroi, 7);

            for (let i = 2; i < 7; i++) {
                cartasDoOponente[i] = cartasDoHeroi[i];
            }

            let derrotado = false;

            // Quantos oponentes empataram com o herói. Sem esse número não há como
            // saber em quantas partes o pote foi dividido.
            let empatados = 0;

            for (let o = 0; o < oponentes; o++) {

                cartasDoOponente[0] = restantes[o * 2];
                cartasDoOponente[1] = restantes[o * 2 + 1];

                const comparacao = pontosDoHeroi - avaliarMelhor(cartasDoOponente, 7);

                // Basta um oponente melhor para o herói não levar nada
                if (comparacao < 0) {
                    derrotado = true;
                    break;
                }

                if (comparacao === 0) {
                    empatados++;
                }
            }

            if (derrotado) {
                losses++;
            } else if (empatados > 0) {
                ties++;
                equitySum += 1.0 / (empatados + 1);   // divide o pote entre herói e empatados
            } else {
                wins++;
                equitySum += 1.0;
            }
        }

        return {
            wins: wins,
            losses: losses,
            ties: ties,
            simulations: simulacoes,
            equitySum: equitySum
        };
    }

    // ===== RECOMENDAÇÃO DE JOGADA =====

    /** Ponto de equilíbrio: abaixo desta equity, pagar dá prejuízo no longo prazo. */
    function equityNecessaria(pot, toCall) {
        if (toCall === 0) {
            return 0;
        }
        return (toCall * 100.0) / (pot + toCall);
    }

    /**
     * Acima desta equity, a jogada agressiva rende mais que a passiva.
     * Contra 1 oponente dá 50%; contra 2, 33,3%; contra 3, 25%.
     */
    function limiteDeAgressao(oponentes) {
        return 100.0 / (oponentes + 1);
    }

    /** EV de continuar na mão, em fichas. Positivo significa lucro médio no longo prazo. */
    function valorEsperado(equityPercentual, pot, toCall) {
        const equity = equityPercentual / 100.0;
        return equity * pot - (1 - equity) * toCall;
    }

    function decidir(equityPercentual, necessaria, limite, toCall) {

        // Sem aposta na frente, continuar não custa nada: desistir nunca é a melhor jogada
        if (toCall === 0) {
            return equityPercentual > limite ? 'BET' : 'CHECK';
        }

        // O preço vem antes da força da mão: por pior que seja o desconto,
        // uma aposta grande o bastante torna qualquer mão impagável
        if (equityPercentual < necessaria) {
            return 'FOLD';
        }

        return equityPercentual > limite ? 'RAISE' : 'CALL';
    }

    function aconselhar(equityPercentual, pot, toCall, oponentes) {

        const necessaria = equityNecessaria(pot, toCall);
        const limite = limiteDeAgressao(oponentes);

        return {
            decision: decidir(equityPercentual, necessaria, limite, toCall),
            equityPercentage: equityPercentual,
            requiredEquityPercentage: necessaria,
            aggressionThresholdPercentage: limite,
            expectedValue: valorEsperado(equityPercentual, pot, toCall)
        };
    }

    global.PokerEngine = {
        indiceDaCarta: indiceDaCarta,
        avaliarMelhor: avaliarMelhor,
        suportaEnumeracao: suportaEnumeracao,
        enumerar: enumerar,
        simularBloco: simularBloco,
        aconselhar: aconselhar
    };

})(typeof self !== 'undefined' ? self : this);
