/**
 * Worker de simulação.
 *
 * Cada worker é uma thread de verdade do navegador e roda um bloco do total de
 * simulações, equivalente a um chunk do parallelStream no back-end. Manter o cálculo
 * fora da thread principal é o que impede a página de congelar durante a conta.
 */

importScripts('poker-engine.js');

self.addEventListener('message', function (evento) {

    const pedido = evento.data;

    try {
        const resultado = self.PokerEngine.simularBloco(
            pedido.mao,
            pedido.cartasDaMesa,
            pedido.oponentes,
            pedido.simulacoes
        );

        self.postMessage({ id: pedido.id, resultado: resultado });

    } catch (erro) {
        self.postMessage({ id: pedido.id, erro: erro.message });
    }
});
