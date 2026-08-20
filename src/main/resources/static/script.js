/* ============================================================
   1. Dados do baralho
      'value' e o nome do enum Java (Rank / Suit) que a API espera.
      'label' e 'symbol' sao so o que o usuario ve.
   ============================================================ */
const RANKS = [
    { value: 'TWO',   label: '2'  },
    { value: 'THREE', label: '3'  },
    { value: 'FOUR',  label: '4'  },
    { value: 'FIVE',  label: '5'  },
    { value: 'SIX',   label: '6'  },
    { value: 'SEVEN', label: '7'  },
    { value: 'EIGHT', label: '8'  },
    { value: 'NINE',  label: '9'  },
    { value: 'TEN',   label: '10' },
    { value: 'JACK',  label: 'J'  },
    { value: 'QUEEN', label: 'Q'  },
    { value: 'KING',  label: 'K'  },
    { value: 'ACE',   label: 'A'  }
];

const SUITS = [
    { value: 'HEARTS',   symbol: '\u2665', nome: 'Copas',    cor: 'is-red'   },
    { value: 'DIAMONDS', symbol: '\u2666', nome: 'Ouros',    cor: 'is-red'   },
    { value: 'CLUBS',    symbol: '\u2663', nome: 'Paus',     cor: 'is-black' },
    { value: 'SPADES',   symbol: '\u2660', nome: 'Espadas',  cor: 'is-black' }
];

const ROTULOS_HERO  = ['Carta 1', 'Carta 2'];
const ROTULOS_BOARD = ['Flop 1', 'Flop 2', 'Flop 3', 'Turn', 'River'];

/* ============================================================
   2. O estado — a unica fonte da verdade.
      Cada posicao guarda { rank, suit } ou null. Nao existe
      meio-termo, e por isso "carta pela metade" deixa de existir.
   ============================================================ */
const estado = {
    hero:  [null, null],
    board: [null, null, null, null, null]
};

/* ============================================================
   3. Tradutores: nome do enum -> o que aparece na tela
   ============================================================ */
function buscarRank(valor) {
    return RANKS.find(function (r) { return r.value === valor; });
}

function buscarSuit(valor) {
    return SUITS.find(function (s) { return s.value === valor; });
}

/* ============================================================
   4. Render
      Monta o HTML de um slot a partir do estado dele.
      data-pos identifica a posicao ("hero-0", "board-3") e e
      o que o Passo 5 vai ler quando o slot for clicado.
   ============================================================ */
function montarSlot(carta, area, indice, rotulo) {

    const posicao = area + '-' + indice;

    if (carta === null) {
        return `<button type="button" class="slot" data-pos="${posicao}"
                        aria-label="${rotulo}: vazio, clique para escolher"></button>`;
    }

    const rank = buscarRank(carta.rank);
    const suit = buscarSuit(carta.suit);

    return `<button type="button" class="slot is-filled ${suit.cor}" data-pos="${posicao}"
                    aria-label="${rotulo}: ${rank.label} de ${suit.nome}">
                <span class="rank">${rank.label}</span><span class="suit">${suit.symbol}</span>
            </button>`;
}

const heroSlots  = document.getElementById('hero-slots');
const boardSlots = document.getElementById('community-slots');

/* Redesenha as duas fileiras inteiras a partir do estado.
   Toda mudanca no estado termina numa chamada a render(). */
function render() {

    heroSlots.innerHTML = estado.hero
        .map(function (carta, i) { return montarSlot(carta, 'hero', i, ROTULOS_HERO[i]); })
        .join('');

    boardSlots.innerHTML = estado.board
        .map(function (carta, i) { return montarSlot(carta, 'board', i, ROTULOS_BOARD[i]); })
        .join('');
}

/* Primeira pintura: sem isto a mesa nasce vazia. */
render();

/* ============================================================
   5. Modal do baralho
   ============================================================ */
const overlay   = document.getElementById('deck-overlay');
const deckGrid  = document.getElementById('deck-grid');
const deckTitle = document.getElementById('deck-title');

// Qual slot esta sendo editado agora. null = modal fechado.
let posicaoAberta = null;

/* Conjunto das cartas ja escolhidas, no formato "ACE-SPADES".
   Set porque has() e O(1) — vou consultar 52 vezes por abertura. */
function cartasEmUso() {

    const usadas = new Set();

    estado.hero.concat(estado.board).forEach(function (carta) {
        if (carta !== null) {
            usadas.add(carta.rank + '-' + carta.suit);
        }
    });

    return usadas;
}

/* Desenha as 52 cartas. As que ja estao na mesa saem disabled —
   e por isso que carta repetida deixa de ser possivel (bug 5). */
function montarBaralho() {

    const usadas = cartasEmUso();
    const atual  = estado[posicaoAberta.area][posicaoAberta.indice];

    // A carta que ja esta neste slot nao conta como ocupada: ela e dele.
    if (atual !== null) {
        usadas.delete(atual.rank + '-' + atual.suit);
    }

    let html = '';

    SUITS.forEach(function (suit) {
        RANKS.forEach(function (rank) {

            const chave       = rank.value + '-' + suit.value;
            const bloqueada   = usadas.has(chave);
            const selecionada = atual !== null
                             && atual.rank === rank.value
                             && atual.suit === suit.value;

            html += `<button type="button"
                             class="deck-card ${suit.cor}${selecionada ? ' is-selected' : ''}"
                             data-rank="${rank.value}"
                             data-suit="${suit.value}"
                             ${bloqueada ? 'disabled' : ''}
                             aria-label="${rank.label} de ${suit.nome}">
                        <span class="rank">${rank.label}</span><span class="suit">${suit.symbol}</span>
                     </button>`;
        });
    });

    deckGrid.innerHTML = html;
}

function abrirBaralho(area, indice) {

    posicaoAberta = { area: area, indice: indice };

    const rotulo = (area === 'hero') ? ROTULOS_HERO[indice] : ROTULOS_BOARD[indice];
    deckTitle.textContent = 'Escolher \u2014 ' + rotulo;

    montarBaralho();
    overlay.hidden = false;
}

function fecharBaralho() {
    overlay.hidden = true;
    posicaoAberta = null;
}

/* ============================================================
   6. Eventos — delegacao
      Um listener no container em vez de um por elemento. Sobrevive
      ao render(), que destroi e recria os filhos.
   ============================================================ */
function aoClicarNoSlot(event) {

    // target pode ser o <span> de dentro; closest() sobe ate o botao.
    const slot = event.target.closest('.slot');

    if (slot === null) {
        return;
    }

    const partes = slot.dataset.pos.split('-');   // "board-3" -> ["board", "3"]
    abrirBaralho(partes[0], Number(partes[1]));
}

heroSlots.addEventListener('click', aoClicarNoSlot);
boardSlots.addEventListener('click', aoClicarNoSlot);

deckGrid.addEventListener('click', function (event) {

    const carta = event.target.closest('.deck-card');

    if (carta === null || carta.disabled) {
        return;
    }

    // Muda o estado, redesenha, fecha. Nunca mexe no DOM da mesa direto.
    estado[posicaoAberta.area][posicaoAberta.indice] = {
        rank: carta.dataset.rank,
        suit: carta.dataset.suit
    };

    render();
    fecharBaralho();
});

document.getElementById('deck-clear').addEventListener('click', function () {
    estado[posicaoAberta.area][posicaoAberta.indice] = null;
    render();
    fecharBaralho();
});

document.getElementById('deck-close').addEventListener('click', fecharBaralho);

/* Clique no fundo escuro fecha; clique dentro do dialogo nao.
   currentTarget e sempre quem tem o listener (o overlay);
   target e o que foi clicado de fato. Se sao o mesmo, o clique
   foi no fundo. */
overlay.addEventListener('click', function (event) {
    if (event.target === event.currentTarget) {
        fecharBaralho();
    }
});

document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape' && !overlay.hidden) {
        fecharBaralho();
    }
});

/* ============================================================
   7. Leitura dos controles
   ============================================================ */
const form      = document.getElementById('odds-form');
const botao     = document.getElementById('calcular');
const resultado = document.getElementById('resultado');

function lerNumeroOpcional(id) {
    const valor = document.getElementById(id).value.trim();
    return valor === '' ? null : Number(valor);
}

/* ============================================================
   8. Validacao — antes de gastar um round-trip com o servidor.
      O backend valida de novo (nunca confie no cliente), mas aqui
      a mensagem e imediata e diz exatamente o que esta faltando.
   ============================================================ */
function validar() {

    if (estado.hero[0] === null || estado.hero[1] === null) {
        return 'Escolha as duas cartas da sua m\u00e3o.';
    }

    // Quantas cartas seguidas, a partir do inicio, estao preenchidas
    let preenchidas = 0;
    while (preenchidas < estado.board.length && estado.board[preenchidas] !== null) {
        preenchidas++;
    }

    // Se sobrou carta depois do primeiro buraco, o usuario pulou uma rua
    for (let i = preenchidas; i < estado.board.length; i++) {
        if (estado.board[i] !== null) {
            return 'Preencha a mesa em ordem: flop completo, depois turn, depois river.';
        }
    }

    if (preenchidas === 1 || preenchidas === 2) {
        return 'O flop tem 3 cartas. Complete as tr\u00eas ou deixe a mesa vazia.';
    }

    const pot    = lerNumeroOpcional('pot');
    const toCall = lerNumeroOpcional('to-call');

    // (a === null) !== (b === null) e verdadeiro quando so um dos dois esta vazio
    if ((pot === null) !== (toCall === null)) {
        return 'Informe o pote e o valor a pagar juntos, ou deixe os dois em branco.';
    }

    return null;   // null = tudo certo
}

/* ============================================================
   9. Payload — o JSON que vira OddsRequest no Java
   ============================================================ */
function montarPayload() {

    const payload = {
        heroCards: estado.hero,
        communityCards: estado.board.filter(function (c) { return c !== null; }),
        opponents:   Number(document.getElementById('opponents').value),
        simulations: Number(document.getElementById('simulations').value)
    };

    const pot    = lerNumeroOpcional('pot');
    const toCall = lerNumeroOpcional('to-call');

    if (pot !== null) {
        payload.pot = pot;
        payload.toCall = toCall;
    }

    return payload;
}

/* ============================================================
   10. Mensagens
       textContent (nao innerHTML): o texto nunca e interpretado
       como HTML, entao nao ha como injetar marcacao pela mensagem.
   ============================================================ */
function mostrarMensagem(classe, texto) {
    const div = document.createElement('div');
    div.className = classe;
    div.textContent = texto;
    resultado.replaceChildren(div);
}

/* ============================================================
   12. Painel de resultado
       O card de decisao e IRMAO do card de resultado, nao filho.
       Era esse aninhamento que quebrava o layout antes.
   ============================================================ */
function mostrarResultado(data) {

    const quantidade = data.simulations.toLocaleString('pt-BR');

    const origem = data.exact
        ? `Resultado exato \u2014 todas as ${quantidade} combina\u00e7\u00f5es foram avaliadas`
        : `Estimativa \u2014 ${quantidade} simula\u00e7\u00f5es`;

    resultado.innerHTML = `
        <div class="result-card">

            <div class="equity">
                <span class="equity-label">Equity</span>
                <span class="equity-value">${data.equityPercentage.toFixed(1)}%</span>
            </div>

            <div class="equity-bar">
                <span class="bar-win"  style="width: ${data.winPercentage}%"></span>
                <span class="bar-tie"  style="width: ${data.tiePercentage}%"></span>
                <span class="bar-loss" style="width: ${data.lossPercentage}%"></span>
            </div>

            <div class="stats">
                <div class="stat win">
                    <span class="stat-value">${data.winPercentage.toFixed(2)}%</span>
                    <span class="stat-label">Vit\u00f3ria</span>
                </div>
                <div class="stat tie">
                    <span class="stat-value">${data.tiePercentage.toFixed(2)}%</span>
                    <span class="stat-label">Empate</span>
                </div>
                <div class="stat loss">
                    <span class="stat-value">${data.lossPercentage.toFixed(2)}%</span>
                    <span class="stat-label">Derrota</span>
                </div>
            </div>

            <p class="result-source ${data.exact ? 'exact' : ''}">${origem}</p>

        </div>

        ${montarDecisao(data.decision)}
    `;
}

/* ============================================================
   13. Card de recomendacao
   ============================================================ */
const ACOES = {
    FOLD:  { rotulo: 'DESISTIR', classe: 'fold'  },
    CHECK: { rotulo: 'CHECAR',   classe: 'check' },
    CALL:  { rotulo: 'PAGAR',    classe: 'call'  },
    BET:   { rotulo: 'APOSTAR',  classe: 'bet'   },
    RAISE: { rotulo: 'AUMENTAR', classe: 'bet'   }
};

function montarDecisao(decision) {

    // A API omite 'decision' quando pote e valor a pagar nao foram informados
    if (!decision) {
        return '';
    }

    const acao = ACOES[decision.decision];

    // Defensivo: se algum dia entrar um valor novo no enum Decision do Java,
    // a tela deixa de mostrar o card em vez de quebrar com "undefined.classe"
    if (!acao) {
        return '';
    }

    const preco = decision.requiredEquityPercentage > 0
        ? `precisa de ${decision.requiredEquityPercentage.toFixed(1)}% para o pagamento compensar`
        : 'n\u00e3o h\u00e1 aposta a pagar';

    const sinal   = decision.expectedValue >= 0 ? '+' : '';
    const evCor   = decision.expectedValue >= 0 ? 'ev-positivo' : 'ev-negativo';

    return `
        <div class="decision-card ${acao.classe}">
            <span class="decision-action">${acao.rotulo}</span>
            <p class="decision-detail">Sua equity \u00e9 <strong>${decision.equityPercentage.toFixed(1)}%</strong> e ${preco}.</p>
            <p class="decision-detail">Acima de ${decision.aggressionThresholdPercentage.toFixed(1)}% a jogada agressiva rende mais que a passiva.</p>
            <p class="decision-detail">EV de continuar: <strong class="${evCor}">${sinal}${decision.expectedValue.toFixed(2)}</strong> fichas</p>
        </div>
    `;
}
/* ============================================================
   11. Envio
   ============================================================ */
form.addEventListener('submit', async function (event) {

    // Sem isto o navegador recarrega a pagina e nada disso roda
    event.preventDefault();

    const erro = validar();

    if (erro !== null) {
        mostrarMensagem('warn-card', erro);
        return;
    }

    // Trava o botao: com 200.000 simulacoes da tempo de clicar 5 vezes (bug 7)
    botao.disabled = true;
    botao.textContent = 'Calculando\u2026';
    mostrarMensagem('status-card', 'Rodando a simula\u00e7\u00e3o\u2026');

    try {
        const response = await fetch('/api/odds', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(montarPayload())
        });

        const data = await response.json();

        // fetch NAO lanca em 400/500 — a checagem tem que ser explicita
        if (!response.ok) {
            const detalhes = (data.details && data.details.length > 0)
                ? data.details.join(' \u00b7 ')
                : data.message;
            mostrarMensagem('error-card', detalhes);
            return;
        }

        mostrarResultado(data);

    } catch (e) {
        // So cai aqui se a rede falhar ou o servidor estiver fora
        mostrarMensagem('error-card', 'N\u00e3o foi poss\u00edvel falar com o servidor.');

    } finally {
        // Roda sempre, inclusive nos returns acima
        botao.disabled = false;
        botao.textContent = 'Calcular odds';
    }
});