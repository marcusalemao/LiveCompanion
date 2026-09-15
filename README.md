# Live Companion

Assistente multimodal para óculos Rokid: vê o que eu vejo via streaming, conversa sobre tudo, lembra onde deixei chaves, carro, moto ou bicicleta, com quem e sobre o que conversei — e me lembra das minhas tarefas e eventos.

## Por que este projeto existe

O Vision AI nativo dos Rokid é rápido (câmera + reconhecimento imediato), mas **não tem memória**: cada sessão começa do zero — não sabe meu nome, não lembra onde deixei as chaves, com quem conversei. Este app resolve isso encaminhando o vídeo para um assistente com memória persistente (Beto, Superagent do Base44).

## O que ele faz

- **Vê comigo**: streaming constante da câmera dos óculos quando ativado.
- **Conversa sobre tudo**: perguntas por voz, respostas curtas no visor + TTS.
- **Lembra dos meus objetos**: onde deixei chaves, carro, moto, carteira, documentos.
- **Lembra dos lugares**: por onde passei no dia — o café, a reunião naquele andar, a loja.
- **Lembra das pessoas**: com quem conversei e o resumo da conversa — acordos, tópicos e pendências. Funciona MESMO sem reconhecimento facial: a interação fica identificada por âncoras visuais transitórias (ex: "Pessoa 1, casaco vermelho, 11:43, Av. Paulista") + horário + local + resumo do que foi falado.
- **Agenda**: lembretes das minhas tarefas e eventos do Google.

## Três tipos de memória

| Tipo | Entidade | Exemplo |
|------|----------|---------|
| Objetos | `EpisodicMemory` (objeto_pessoal, veiculo, documento) | "Chaves no bolso da jaqueta cinza, em casa, 8h10" |
| Lugares | `EpisodicMemory` (lugar) + coordenadas | "Reunião no 7º andar do edifício X, 14h" |
| Pessoas | `PersonInteraction` | "Pessoa 1, casaco vermelho, 11:43, Av. Paulista — falamos do orçamento, ficou de mandar proposta até sexta" |

A memória de pessoas **não depende de reconhecimento facial**: cada interação ganha um identificador temporário (`temp_identifier`), âncoras visuais (`visual_anchors`: roupa, óculos, acessórios), janela de tempo (`start_time`/`end_time`) e resumo da conversa (`conversation_summary`). Se o rosto for reconhecido depois (via Vision), a interação é ligada à pessoa permanente.

## Arquitetura (visão de alto nível)

```
┌──────────────┐     stream de vídeo      ┌──────────────┐     consulta      ┌─────────────┐
│  Óculos Rokid │  ─────────────────────►  │   Servidor   │  ──────────────►  │  Beto (AI)  │
│  (app Android │   (frames via WebSocket) │ (Oracle Cloud│    API REST       │  Base44 API │
│   próprio)    │                          │  Free Tier)  │                   │  + memória  │
└──────────────┘                          └──────────────┘                   └─────────────┘
```

Detalhes completos em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Regras de desenvolvimento (validadas na prática)

Testado em Samsung Galaxy Z Fold / Rokid (Android 16, One UI 8.5, Knox 3.13):

- Interação **100% por KeyEvents** de hardware (Power, Back, Menu, Volume) — nunca touch.
- HUD fixo em **paisagem 640x480**, texto curto no visor (sem scroll).
- TTS **nativo do Android/Samsung** para latência mínima offline.
- Wake word offline **"oi Beto"**.
- `getUserMedia`: câmera traseira primeiro (`facingMode: environment`), mostrar estado na tela antes de chamar.

## Roadmap

- [ ] Fase 0 — Esqueleto do app Android (Kotlin + Gradle)
- [ ] Fase 1 — Captura de câmera local nos óculos (sem touch, via KeyEvent)
- [ ] Fase 2 — Envio de frames ao servidor (buffer circular, WebSocket)
- [ ] Fase 3 — Integração com o assistente (API Base44, memória persistente)
- [ ] Fase 4 — Memória do dia: objetos (chaves/carro/moto), lugares visitados e interações com pessoas (âncoras visuais + resumo da conversa, sem depender de reconhecimento facial)
- [ ] Fase 5 — Tarefas e eventos do Google (lembretes)
- [ ] Fase 6 — Tethering via Galaxy Watch 7 Pro (rede própria)

## Status

**Fase 0 — esqueleto do app.** Repositório privado.

Nome oficial decidido em 15/09/2026: **Live Companion** (ex-RokidLive). Spec oficial de produto em [docs/live-companion-spec.md](docs/live-companion-spec.md).

## Relacionados

- [Vision](https://github.com/marcusalemao/Vision) (ex-facecontext) — emulador AR / reconhecimento facial.
- [rokid-livestream](https://github.com/marcusalemao/rokid-livestream) — experimento de streaming constante câmera → servidor → assistente.
