/**
 * Orquestra o cálculo no navegador — o papel que o OddsController tinha na API.
 *
 * Valida a entrada, escolhe entre enumeração exata e Monte Carlo, divide o trabalho
 * entre os núcleos disponíveis e monta a mesma resposta que o back-end devolvia.
 * Como o contrato de saída não mudou, a camada de exibição continua igual.
 */
(function (global) {
    'use strict';

    const Engine = global.PokerEngine;

    // Resolvido a partir da URL deste script, e não da página: assim continua correto
    // se o site for servido de um subdiretório, como acontece no GitHub Pages
    const URL_DO_WORKER = new URL('poker-worker.js',
        (document.currentScript && document.currentScript.src) || location.href).href;

    // Acima disso o ganho some e só sobra o custo de criar as threads
    const MAXIMO_DE_WORKERS = 8;

    let pool = null;
    let contadorDePedidos = 0;

    function nucleosDisponiveis() {
        const nucleos = navigator.hardwareConcurrency || 4;
        return Math.max(1, Math.min(nucleos, MAXIMO_DE_WORKERS));
    }

    /**
     * Cria os workers na primeira conta e reaproveita nas seguintes: subir uma thread
     * custa dezenas de milissegundos, o que pesaria em cada clique.
     *
     * Devolve null quando não dá para usar worker — abrir o index.html direto do disco,
     * por exemplo, cai nesse caso — e aí o cálculo roda na própria thread da página.
     */
    function obterPool() {

        if (pool !== null) {
            return pool.length > 0 ? pool : null;
        }

        pool = [];

        if (typeof Worker !== 'function') {
            return null;
        }

        try {
            for (let i = 0; i < nucleosDisponiveis(); i++) {
                pool.push(new Worker(URL_DO_WORKER));
            }
        } catch (erro) {
            for (const worker of pool) {
                worker.terminate();
            }
            pool = [];
            return null;
        }

        return pool;
    }

    function pedirBloco(worker, mensagem) {
        return new Promise(function (resolve, reject) {

            const id = ++contadorDePedidos;

            function desligar() {
                worker.removeEventListener('message', aoResponder);
                worker.removeEventListener('error', aoFalhar);
            }

            function aoResponder(evento) {
                if (evento.data.id !== id) {
                    return;
                }
                desligar();

                if (evento.data.erro) {
                    reject(new Error(evento.data.erro));
                } else {
                    resolve(evento.data.resultado);
                }
            }

            function aoFalhar(evento) {
                desligar();
                reject(new Error(evento.message || 'Falha ao simular em paralelo'));
            }

            worker.addEventListener('message', aoResponder);
            worker.addEventListener('error', aoFalhar);

            mensagem.id = id;
            worker.postMessage(mensagem);
        });
    }

    /** Divide o total em blocos de tamanho equilibrado, um por núcleo. */
    function dividirEmBlocos(simulacoes, blocos) {

        const base = Math.floor(simulacoes / blocos);
        const resto = simulacoes % blocos;
        const tamanhos = [];

        for (let i = 0; i < blocos; i++) {
            tamanhos.push(i < resto ? base + 1 : base);
        }

        return tamanhos;
    }

    function somarParciais(parciais, simulacoes) {

        let wins = 0;
        let losses = 0;
        let ties = 0;
        let equitySum = 0.0;

        for (const parcial of parciais) {
            wins += parcial.wins;
            losses += parcial.losses;
            ties += parcial.ties;
            equitySum += parcial.equitySum;
        }

        return {
            wins: wins,
            losses: losses,
            ties: ties,
            simulations: simulacoes,
            equitySum: equitySum
        };
    }

    async function simular(mao, cartasDaMesa, oponentes, simulacoes) {

        const workers = obterPool();

        if (workers === null) {
            return Engine.simularBloco(mao, cartasDaMesa, oponentes, simulacoes);
        }

        const tamanhos = dividirEmBlocos(simulacoes, Math.min(workers.length, simulacoes));

        const parciais = await Promise.all(tamanhos.map(function (tamanho, i) {
            return pedirBloco(workers[i], {
                mao: mao,
                cartasDaMesa: cartasDaMesa,
                oponentes: oponentes,
                simulacoes: tamanho
            });
        }));

        return somarParciais(parciais, simulacoes);
    }

    // ===== VALIDAÇÃO =====
    //
    // Reproduz as mesmas mensagens que as anotações do Bean Validation e as checagens
    // do controller devolviam, no mesmo formato do ApiError, para que a exibição de
    // erro da página não precise mudar.

    function erroDeValidacao(mensagem, detalhes) {
        const erro = new Error(mensagem);

        erro.apiError = {
            status: 400,
            message: mensagem,
            details: detalhes || [],
            timestamp: new Date().toISOString()
        };

        return erro;
    }

    function validarCampos(requisicao) {

        const detalhes = [];

        const cartasDoHeroi = requisicao.heroCards || [];

        if (cartasDoHeroi.length !== 2 || cartasDoHeroi.some(function (c) { return !c; })) {
            detalhes.push('O herói deve ter exatamente 2 cartas');
        }

        if ((requisicao.communityCards || []).length > 5) {
            detalhes.push('No máximo 5 cartas comunitárias');
        }

        if (requisicao.opponents < 1) {
            detalhes.push('Deve haver pelo menos 1 oponente');
        }

        if (requisicao.opponents > 9) {
            detalhes.push('No máximo 9 oponentes');
        }

        if (requisicao.simulations < 1000) {
            detalhes.push('Use pelo menos 1000 simulações');
        }

        if (requisicao.simulations > 200000) {
            detalhes.push('No máximo 200.000 simulações');
        }

        if (requisicao.pot !== undefined && requisicao.pot !== null && requisicao.pot <= 0) {
            detalhes.push('O pote deve ser maior que zero');
        }

        if (requisicao.toCall !== undefined && requisicao.toCall !== null && requisicao.toCall < 0) {
            detalhes.push('O valor a pagar não pode ser negativo');
        }

        if (detalhes.length > 0) {
            throw erroDeValidacao('Dados inválidos na requisição', detalhes);
        }
    }

    // ===== CÁLCULO =====

    async function calcular(requisicao) {

        validarCampos(requisicao);

        const mao = requisicao.heroCards.map(Engine.indiceDaCarta);
        const cartasDaMesa = (requisicao.communityCards || []).map(Engine.indiceDaCarta);

        const abertas = cartasDaMesa.length;

        if (abertas !== 0 && abertas !== 3 && abertas !== 4 && abertas !== 5) {
            throw erroDeValidacao(
                'Cartas comunitárias devem ter 0 (pré-flop), 3 (flop), 4 (turn) ou 5 (river) cartas'
            );
        }

        const todas = mao.concat(cartasDaMesa);

        if (new Set(todas).size !== todas.length) {
            throw erroDeValidacao('Há cartas repetidas entre sua mão e as cartas da mesa');
        }

        const temPote = requisicao.pot !== undefined && requisicao.pot !== null;
        const temPagamento = requisicao.toCall !== undefined && requisicao.toCall !== null;

        // pote e valor a pagar andam juntos: com apenas um deles não há como calcular o preço
        if ((temPote || temPagamento) && !(temPote && temPagamento)) {
            throw erroDeValidacao('Informe o pote e o valor a pagar juntos, ou nenhum dos dois');
        }

        const exact = Engine.suportaEnumeracao(cartasDaMesa, requisicao.opponents);

        const odds = exact
            ? Engine.enumerar(mao, cartasDaMesa)
            : await simular(mao, cartasDaMesa, requisicao.opponents, requisicao.simulations);

        const winPercentage = (odds.wins * 100.0) / odds.simulations;
        const lossPercentage = (odds.losses * 100.0) / odds.simulations;
        const tiePercentage = (odds.ties * 100.0) / odds.simulations;

        // Vem da soma das frações do pote, e não de win% + tie%/2: num empate a três
        // o herói leva 1/3, e essa informação se perderia ao derivar de ties
        const equityPercentage = (odds.equitySum * 100.0) / odds.simulations;

        const decision = temPote
            ? Engine.aconselhar(equityPercentage, requisicao.pot, requisicao.toCall, requisicao.opponents)
            : null;

        return {
            wins: odds.wins,
            losses: odds.losses,
            ties: odds.ties,
            simulations: odds.simulations,
            winPercentage: winPercentage,
            lossPercentage: lossPercentage,
            tiePercentage: tiePercentage,
            equityPercentage: equityPercentage,
            exact: exact,
            decision: decision
        };
    }

    global.PokerRunner = { calcular: calcular };

})(window);
