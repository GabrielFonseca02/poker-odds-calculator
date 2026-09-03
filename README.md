# Poker Odds Calculator

Calculadora de probabilidades para Texas Hold'em. Informe sua mão e as cartas da mesa, e a aplicação estima sua chance real de vitória através de simulação de Monte Carlo.

Funciona em todas as fases da mão — pré-flop, flop, turn e river — permitindo acompanhar como suas chances mudam conforme as cartas abrem. Informando o tamanho do pote e o valor a pagar, ela vai além da probabilidade e recomenda a jogada.

![Interface da calculadora mostrando equity e recomendação de jogada no flop](docs/screenshot.png)

---

## Funcionalidades

- Cálculo de equity contra 1 a 9 oponentes
- Suporte às quatro fases da mão (pré-flop, flop, turn, river)
- **Recomendação de jogada** a partir de pot odds e valor esperado: desistir, checar, pagar, apostar ou aumentar
- Tratamento correto de empates multiway, em que o pote se divide entre todos os empatados
- Avaliação completa de mãos de poker, incluindo desempate por kicker e a sequência A-2-3-4-5 com o Ás valendo 1
- Resposta **exata** no river contra um oponente, por enumeração completa em vez de amostragem
- Simulação paralelizada, adaptando-se automaticamente ao número de núcleos disponíveis
- API REST com validação de entrada e respostas de erro padronizadas
- Interface web sem dependências externas de front-end

## Stack

| Camada | Tecnologia |
|---|---|
| Back-end | Java 17+, Spring Boot 4 |
| Build | Maven |
| Testes | JUnit 5 |
| Front-end | HTML, CSS e JavaScript puro (sem framework) |

O front-end é servido pelo próprio Spring Boot a partir de `src/main/resources/static`, o que mantém API e interface na mesma origem e dispensa configuração de CORS.

Essa mesma pasta é publicada no GitHub Pages. Para que a página funcione sem back-end no ar, a lógica de cálculo existe duas vezes: em Java, exposta pela API REST, e em JavaScript (`poker-engine.js`), rodando no navegador. As duas seguem os mesmos critérios de classificação, desempate e equity — no river contra um oponente, onde ambas enumeram todas as 990 combinações, devolvem resultados idênticos carta a carta.

As cartas são escolhidas em um seletor visual com o baralho completo, e não em campos de texto. A escolha não é estética: cartas já em uso aparecem desabilitadas, e valor e naipe são selecionados como uma coisa só. Isso torna carta repetida e carta pela metade **impossíveis de representar** na interface, em vez de erros a serem validados depois.

## No ar

https://gabrielfonseca02.github.io/poker-odds-calculator/

O deploy é automático: o workflow em `.github/workflows/pages.yml` publica a pasta `src/main/resources/static` a cada push na `main`. Não existe uma segunda cópia do front-end — o Pages serve exatamente os mesmos arquivos que o Spring Boot entrega localmente.

Como o cálculo roda no navegador de quem acessa, não há servidor para hibernar, cold start nem custo de hospedagem. A simulação é distribuída entre os núcleos da máquina do visitante com Web Workers, seguindo o mesmo particionamento que o `MonteCarloSimulation` faz com `parallelStream` no back-end.

## Como rodar

Pré-requisito: JDK 17 ou superior.

```bash
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd`. Se o Maven reclamar de `JAVA_HOME`, aponte a variável para a sua instalação do JDK antes de rodar.

Depois disso, acesse:

```
http://localhost:8080
```

Para rodar os testes:

```bash
./mvnw test
```

## API

### `POST /api/odds`

**Requisição**

```json
{
  "heroCards": [
    { "rank": "ACE", "suit": "HEARTS" },
    { "rank": "ACE", "suit": "SPADES" }
  ],
  "communityCards": [
    { "rank": "KING", "suit": "HEARTS" },
    { "rank": "TWO", "suit": "CLUBS" },
    { "rank": "SEVEN", "suit": "DIAMONDS" }
  ],
  "opponents": 1,
  "simulations": 10000,
  "pot": 120,
  "toCall": 40
}
```

| Campo | Regra |
|---|---|
| `heroCards` | Exatamente 2 cartas |
| `communityCards` | 0 (pré-flop), 3 (flop), 4 (turn) ou 5 (river). Pode ser omitido |
| `opponents` | Entre 1 e 9 |
| `simulations` | Entre 1.000 e 200.000 |
| `pot` | Opcional. Maior que zero. Pote atual, já incluindo a aposta do oponente |
| `toCall` | Opcional. Zero ou maior. Quanto custa continuar na mão |

`pot` e `toCall` andam juntos: com apenas um dos dois não há como calcular o preço, e a requisição é rejeitada. Omitindo os dois, a resposta traz apenas as probabilidades, sem recomendação.

Valores de `rank`: `TWO` a `TEN`, `JACK`, `QUEEN`, `KING`, `ACE`.
Valores de `suit`: `HEARTS`, `DIAMONDS`, `CLUBS`, `SPADES`.

**Resposta — 200 OK**

```json
{
  "wins": 8897,
  "losses": 1092,
  "ties": 11,
  "simulations": 10000,
  "winPercentage": 88.97,
  "lossPercentage": 10.92,
  "tiePercentage": 0.11,
  "equityPercentage": 89.025,
  "exact": false,
  "decision": {
    "decision": "RAISE",
    "equityPercentage": 89.025,
    "requiredEquityPercentage": 25.0,
    "aggressionThresholdPercentage": 50.0,
    "expectedValue": 102.43
  }
}
```

O campo `exact` informa como o número foi obtido. Quando `false`, o resultado é uma estimativa por amostragem e `simulations` traz quantas amostras foram usadas. Quando `true`, todas as possibilidades foram percorridas: não há margem de erro, e `simulations` passa a significar o número de combinações avaliadas — 990 no river contra um oponente, independentemente do valor pedido na requisição.

O objeto `decision` é omitido da resposta quando `pot` e `toCall` não são informados.

**Resposta — 400 Bad Request**

```json
{
  "status": 400,
  "message": "Dados inválidos na requisição",
  "details": ["O herói deve ter exatamente 2 cartas"],
  "timestamp": "2026-08-04T14:45:58.503"
}
```

Erros de validação e de domínio (cartas repetidas, quantidade inválida de cartas na mesa) são tratados de forma centralizada por um `@RestControllerAdvice`, garantindo o mesmo formato de resposta em toda a API.

## Da probabilidade à decisão

Saber que sua mão vence 34% das vezes não diz se você deve pagar. O que decide é o **preço**: 34% é uma mão excelente para pagar 10 em um pote de 100, e um erro caro para pagar 200.

O `DecisionAdvisor` traduz equity e tamanho do pote em uma recomendação, a partir de três números.

**Equity mínima para pagar** — o ponto de equilíbrio, abaixo do qual pagar dá prejuízo no longo prazo:

```
equity mínima = toCall / (pot + toCall)
```

**Limiar de agressão** — acima desta equity, apostar rende mais que checar. Assumindo que todos paguem, a diferença entre apostar e checar se reduz a `B × (e × (n+1) − 1)`, cujo sinal depende apenas de `e > 1/(n+1)`:

```
limiar = 1 / (oponentes + 1)
```

Contra 1 oponente dá 50%; contra 2, 33,3%; contra 3, 25%. Faz sentido: quanto mais gente na mão, menos equity você precisa para que apostar valha a pena, porque cada oponente que paga aumenta o que você ganha quando acerta.

**Valor esperado de continuar**, em fichas:

```
EV = equity × pot − (1 − equity) × toCall
```

A recomendação sai da combinação dos três. Sem aposta na frente (`toCall == 0`), desistir nunca é a melhor jogada — resta checar ou apostar, decidido pelo limiar de agressão. Havendo aposta, o preço vem primeiro: por melhor que seja a mão, uma aposta grande o bastante a torna impagável.

### Equity em empates multiway

Equity é a fração do pote que cabe a você no longo prazo. A primeira implementação calculava:

```java
equity = winPercentage + tiePercentage / 2;
```

O `/2` assume que todo empate é entre duas pessoas. Isso vale heads-up, mas num empate a quatro você leva 1/4 do pote, não metade. O erro é sistemático e sempre para cima — e como é justamente esse número que alimenta o `DecisionAdvisor`, a consequência não era um percentual feio na tela: era o app recomendando pagar apostas com valor esperado negativo.

A informação necessária para corrigir não existia no `OddsResult`. Ele guardava "empatou 1.847 vezes", não *com quantos jogadores* cada empate foi — esse dado era descartado dentro do laço, no instante em que o contador era incrementado.

A correção foi parar de descartá-lo. Cada rodada passa a acumular diretamente a fração do pote que coube ao herói:

| Resultado | Fração acumulada |
|---|---|
| Vitória | 1 |
| Empate com `k` oponentes | 1 / (k + 1) |
| Derrota | 0 |

`wins`, `losses` e `ties` continuam existindo para exibição, mas deixaram de ser a base do cálculo. É um caso claro de um número derivado sair errado porque o dado bruto foi agregado cedo demais.

## Performance

O caso mais pesado suportado — 200.000 simulações contra 9 oponentes — levava **91,6 segundos** na versão sequencial, tempo inviável para uma aplicação web.

A simulação foi paralelizada dividindo o total em blocos independentes, um por processador disponível. Cada bloco mantém seus próprios contadores e devolve um resultado parcial; a soma acontece uma única vez, depois que todos terminam. Não há estado mutável compartilhado entre threads, o que dispensa locks e elimina a possibilidade de condições de corrida.

| Versão | Tempo |
|---|---|
| Sequencial | 91.611 ms |
| Paralela | 17.388 ms |
| **Ganho** | **5,3×** |

Medido em um AMD Ryzen 5 5600X (6 núcleos físicos, 12 threads).

O ganho fica abaixo das 12 threads disponíveis por dois motivos: SMT não equivale a núcleos físicos (acrescenta cerca de 20-30% de throughput, não o dobro), e a alocação de objetos por simulação transfere parte do gargalo da CPU para o subsistema de memória e o coletor de lixo.

Os blocos são somados em ordem determinística, por uma única thread, depois que todos terminam. Isso importa porque a equity é acumulada em ponto flutuante, e a soma de `double` depende da ordem das parcelas: mantendo a ordem fixa, duas execuções com a mesma semente produziriam bit a bit o mesmo resultado.

### Enumeração exata no river

A paralelização atacou o custo de rodar as simulações. A otimização seguinte foi eliminar a simulação.

No river não há mais cartas por abrir: a mão do herói já está fechada e a única incógnita são as 2 cartas do oponente, tiradas entre as 45 desconhecidas. São C(45,2) = 990 possibilidades — poucas o bastante para percorrer todas e devolver a resposta certa em vez de estimá-la.

O ganho vem de dois lugares: enumerar 990 combinações em vez de amostrar 200.000, e avaliar a mão do herói uma única vez, fora do laço, em vez de repetir a mesma avaliação a cada iteração.

| Estratégia | Avaliações de mão | Tempo |
|---|---|---|
| Monte Carlo (200.000 amostras) | 400.000 | 6.111,6 ms |
| Enumeração exata | 991 | 37,1 ms |
| **Ganho** | | **164,9×** |

Medido em um Intel Core i7-8550U (4 núcleos físicos, 8 threads), com A♠ A♦ contra um oponente em mesa K♣ 7♥ 2♦ 9♠ 3♣. Os dois tempos são o melhor de várias execuções no mesmo processo, após aquecimento da JIT. É hardware diferente do usado na medição da paralelização, então os tempos das duas tabelas não são comparáveis entre si.

Além de mais rápido, o resultado deixa de ser aproximado: a equity exata é 89,2929%, contra 89,2785% estimados pelo Monte Carlo na mesma execução.

A enumeração roda sequencialmente de propósito. Com apenas 990 iterações, o custo de criar e coordenar threads superaria o do próprio cálculo — paralelizar aqui deixaria mais lento.

O critério de uso é estreito porque o espaço de possibilidades cresce rápido: com 2 oponentes seriam C(45,2) × C(43,2) = 893.970 combinações, já mais caro que o Monte Carlo; com 3, passa de 700 milhões. Por isso a enumeração cobre apenas o river contra um oponente, e todos os demais cenários seguem no Monte Carlo. Quem decide entre as duas estratégias é o `OddsCalculator`, o que mantém o controller alheio a essa escolha.

## Testes

A lógica de avaliação de mãos é coberta por testes unitários, incluindo os casos mais propensos a erro:

- Identificação de royal flush
- Sequência A-2-3-4-5, em que o Ás vale 1 e não 14
- Hierarquia entre categorias (full house vence flush)
- Desempate entre mãos de mesma categoria pelo kicker
- Seleção da melhor combinação de 5 cartas entre 7
- Rejeição de mãos com menos de 5 cartas

Os testes foram escritos antes da paralelização, o que permitiu refatorar a simulação com a garantia de que a lógica de avaliação permanecia intacta.

A enumeração exata trouxe uma vantagem que o Monte Carlo não permite: por ser determinística, aceita asserções sobre números precisos em vez de faixas de tolerância.

- Cobertura das 990 combinações possíveis
- Royal flush imbatível vencendo as 990
- Mesa imbatível (royal flush comunitário) empatando as 990
- Recusa de cenários fora do escopo, em vez de devolver número errado
- Validação cruzada: o resultado exato confere com o Monte Carlo dentro da margem amostral

O último é o mais importante — é ele que garante que a estratégia nova e a antiga concordam entre si.

### Testando um algoritmo aleatório

Monte Carlo devolve um número diferente a cada execução, o que impede asserções sobre valores exatos. Afrouxar a tolerância até o teste parar de falhar resolve a instabilidade e destrói o poder de detecção junto.

A saída foi escolher cenários em que a aleatoriedade não altera o resultado. Com um royal flush comunitário na mesa, nenhuma carta que qualquer jogador possa ter muda nada: todos jogam a mesa e dividem o pote, em toda simulação. O algoritmo continua sorteando, mas a saída é determinística — e a equity tem que ser exatamente `100 / (oponentes + 1)`.

| Oponentes | Equity esperada |
|---|---|
| 1 | 50% |
| 2 | 33,3% |
| 3 | 25% |
| 9 | 10% |

Esse mesmo cenário serve de teste de regressão para o `/2`: a fórmula antiga devolvia 50% em todas as linhas, e falha em três das quatro. Complementam a suíte um teste de compatibilidade (heads-up, a equity continua sendo vitória + metade do empate) e uma invariante que vale para qualquer entrada: a equity nunca fica abaixo da taxa de vitória nem acima de vitória mais empate.

## Estrutura

```
src/main/java/com/GabrielFonseca/pokeroddscalculator/
├── controller/    # Endpoint REST
├── dto/           # Contratos de entrada e saída da API
├── exception/     # Tratamento centralizado de erros
├── model/         # Card, Rank, Suit, Deck, Hand, HandRank, OddsResult,
│                  # Decision e DecisionResult
├── service/       # HandEvaluator (classificação e comparação de mãos) e
│                  # DecisionAdvisor (pot odds, limiar de agressão e EV)
└── simulation/    # OddsCalculator (escolha da estratégia), MonteCarloSimulation
                   # (particionamento e execução paralela) e ExactEnumeration

src/main/resources/static/    # Interface web
src/test/java/                # Testes unitários
```

O `OddsResponse` existe separado do `OddsResult` para que o contrato público da API não fique amarrado ao modelo de domínio: a recomendação de jogada é opcional e não pertence ao resultado de uma simulação, que não sabe nada sobre apostas.

### Como a avaliação de mãos funciona

Uma mão de Texas Hold'em no showdown tem 7 cartas (2 na mão + 5 na mesa), mas apenas 5 valem. O `CombinationGenerator` gera as C(7,5) = 21 combinações possíveis, o `HandEvaluator` classifica cada uma, e a melhor é selecionada.

Cada avaliação devolve a categoria da mão (par, trinca, flush...) junto com os valores de desempate ordenados, permitindo comparar duas mãos da mesma categoria corretamente.

## Próximos passos

- **Enumeração exata no turn**, estendendo o que já funciona no river. Contra um oponente são C(46,2) × 44 = 45.540 combinações — ainda bem abaixo das 200.000 amostras que o Monte Carlo faria, e com resposta exata. O `OddsCalculator` já está estruturado para receber o caso sem alterar o controller
- **Otimização do avaliador de mãos**, hoje o principal consumidor de CPU
- **Range de mãos do oponente** em vez de duas cartas aleatórias, aproximando a simulação de como o jogo é jogado de verdade
- **Deploy** em ambiente público

---

Desenvolvido por [Gabriel Fonseca](https://github.com/GabrielFonseca02).
