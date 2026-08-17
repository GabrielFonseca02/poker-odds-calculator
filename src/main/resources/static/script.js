const RANKS = [
    { value: 'TWO',   label: '2' },
    { value: 'THREE', label: '3' },
    { value: 'FOUR',  label: '4' },
    { value: 'FIVE',  label: '5' },
    { value: 'SIX',   label: '6' },
    { value: 'SEVEN', label: '7' },
    { value: 'EIGHT', label: '8' },
    { value: 'NINE',  label: '9' },
    { value: 'TEN',   label: '10' },
    { value: 'JACK',  label: 'J' },
    { value: 'QUEEN', label: 'Q' },
    { value: 'KING',  label: 'K' },
    { value: 'ACE',   label: 'A' }
];

const SUITS = [
    { value: 'HEARTS',   label: '♥ Copas' },
    { value: 'DIAMONDS', label: '♦ Ouros' },
    { value: 'CLUBS',    label: '♣ Paus' },
    { value: 'SPADES',   label: '♠ Espadas' }
];

const COMMUNITY_IDS = ['flop1', 'flop2', 'flop3', 'turn', 'river'];

function montarOpcoes(lista, permiteVazio) {
    let html = permiteVazio ? '<option value="">—</option>' : '';

    for (const item of lista) {
        html += `<option value="${item.value}">${item.label}</option>`;
    }

    return html;
}

function montarSeletorDeCarta(id, rotulo, permiteVazio) {
    return `
        <div class="card-input">
            <span class="card-input-label">${rotulo}</span>
            <select id="${id}-rank">${montarOpcoes(RANKS, permiteVazio)}</select>
            <select id="${id}-suit">${montarOpcoes(SUITS, permiteVazio)}</select>
        </div>
    `;
}

document.getElementById('hero-cards').innerHTML =
    montarSeletorDeCarta('hero1', 'Carta 1', false) +
    montarSeletorDeCarta('hero2', 'Carta 2', false);

document.getElementById('community-cards').innerHTML =
    montarSeletorDeCarta('flop1', 'Flop 1', true) +
    montarSeletorDeCarta('flop2', 'Flop 2', true) +
    montarSeletorDeCarta('flop3', 'Flop 3', true) +
    montarSeletorDeCarta('turn',  'Turn',   true) +
    montarSeletorDeCarta('river', 'River',  true);

function lerCarta(id) {
    const rank = document.getElementById(`${id}-rank`).value;
    const suit = document.getElementById(`${id}-suit`).value;

    if (rank === '' || suit === '') {
        return null;
    }

    return { rank: rank, suit: suit };
}

function lerCartasDaMesa() {
    const cartas = [];

    for (const id of COMMUNITY_IDS) {
        const carta = lerCarta(id);

        if (carta !== null) {
            cartas.push(carta);
        }
    }

    return cartas;
}

function lerNumeroOpcional(id) {
    const valor = document.getElementById(id).value;
    return valor === '' ? null : Number(valor);
}

document.getElementById('odds-form').addEventListener('submit', async function (event) {
    event.preventDefault();

    const payload = {
        heroCards: [lerCarta('hero1'), lerCarta('hero2')],
        communityCards: lerCartasDaMesa(),
        opponents: Number(document.getElementById('opponents').value),
        simulations: Number(document.getElementById('simulations').value)
    };

    const pot = lerNumeroOpcional('pot');
    const toCall = lerNumeroOpcional('to-call');

    if (pot !== null) payload.pot = pot;
    if (toCall !== null) payload.toCall = toCall;

    const resultado = document.getElementById('resultado');
    resultado.innerHTML = 'Calculando...';

    try {
        const response = await fetch('/api/odds', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (!response.ok) {
            mostrarErro(data);
            return;
        }

        mostrarResultado(data);

    } catch (error) {
        resultado.innerHTML = '<div class="error-card">Não foi possível conectar ao servidor.</div>';
    }
});

function mostrarResultado(data) {
    const resultado = document.getElementById('resultado');

    const quantidade = data.simulations.toLocaleString('pt-BR');

    // Deixa explícito se o número é a resposta certa ou uma estimativa por amostragem
    const origem = data.exact
        ? `Resultado exato — todas as ${quantidade} combinações possíveis foram avaliadas`
        : `Estimativa — ${quantidade} simulações`;

    resultado.innerHTML = `
        <div class="result-card">
            <div class="stat win">
                <span class="stat-value">${data.winPercentage.toFixed(2)}%</span>
                        <p class="result-source ${data.exact ? 'exact' : ''}">${origem}</p>
                        ${montarDecisao(data.decision)}
            </div>
            <div class="stat loss">
                <span class="stat-value">${data.lossPercentage.toFixed(2)}%</span>
                <span class="stat-label">Derrota</span>
            </div>
            <div class="stat tie">
                <span class="stat-value">${data.tiePercentage.toFixed(2)}%</span>
                <span class="stat-label">Empate</span>
            </div>
        </div>
        <p class="result-source ${data.exact ? 'exact' : ''}">${origem}</p>
    `;
}
const ACOES = {
    FOLD:  { rotulo: 'DESISTIR', classe: 'fold' },
    CHECK: { rotulo: 'CHECAR',   classe: 'check' },
    CALL:  { rotulo: 'PAGAR',    classe: 'call' },
    BET:   { rotulo: 'APOSTAR',  classe: 'bet' },
    RAISE: { rotulo: 'SUBIR',    classe: 'bet' }
};

function montarDecisao(decision) {

    if (!decision) {
        return '';
    }

    const acao = ACOES[decision.decision];

    const preco = decision.requiredEquityPercentage > 0
        ? `precisa de ${decision.requiredEquityPercentage.toFixed(1)}% para pagar compensar`
        : 'não há aposta a pagar';

    const sinal = decision.expectedValue >= 0 ? '+' : '';

    return `
        <div class="decision-card ${acao.classe}">
            <span class="decision-action">${acao.rotulo}</span>
            <p class="decision-detail">Sua equity é ${decision.equityPercentage.toFixed(1)}% e ${preco}</p>
            <p class="decision-detail">Acima de ${decision.aggressionThresholdPercentage.toFixed(1)}% a jogada agressiva rende mais</p>
            <p class="decision-detail">EV de continuar: ${sinal}${decision.expectedValue.toFixed(2)} fichas</p>
        </div>
    `;
}
function mostrarErro(data) {
    const resultado = document.getElementById('resultado');

    const detalhes = (data.details && data.details.length > 0)
        ? data.details.join(', ')
        : data.message;

    resultado.innerHTML = `<div class="error-card">${detalhes}</div>`;
}