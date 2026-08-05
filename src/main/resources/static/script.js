document.getElementById('odds-form').addEventListener('submit', async function (event) {
    event.preventDefault();

    const payload = {
        heroCards: [
            { rank: document.getElementById('rank1').value, suit: document.getElementById('suit1').value },
            { rank: document.getElementById('rank2').value, suit: document.getElementById('suit2').value }
        ],
        opponents: Number(document.getElementById('opponents').value),
        simulations: Number(document.getElementById('simulations').value)
    };

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

    resultado.innerHTML = `
        <div class="result-card">
            <div class="stat win">
                <span class="stat-value">${data.winPercentage.toFixed(2)}%</span>
                <span class="stat-label">Vitória</span>
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
    `;
}

function mostrarErro(data) {
    const resultado = document.getElementById('resultado');

    const detalhes = (data.details && data.details.length > 0)
        ? data.details.join(', ')
        : data.message;

    resultado.innerHTML = `<div class="error-card">${detalhes}</div>`;
}