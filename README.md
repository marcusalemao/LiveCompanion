# Live Companion

Assistente multimodal para óculos Rokid: vê o que eu vejo via streaming, conversa sobre tudo, lembra onde deixei chaves, carro, moto ou bicicleta — e me lembra das minhas tarefas e eventos do Google.

## Por que este projeto existe

O Vision AI nativo dos Rokid é rápido (câmera + reconhecimento imediato), mas **não tem memória**: cada sessão começa do zero — não sabe meu nome, não lembra onde deixei as chaves, com quem conversei. Este app resolve isso encaminhando o vídeo para um assistente com memória persistente (Beto, Superagent do Base44).

## O que ele faz

- **Vê comigo**: streaming constante da câmera dos óculos quando ativado.
- **Conversa sobre tudo**: perguntas por voz, respostas curtas no visor + TTS.
- **Lembra do meu dia**: onde deixei chaves/carro/moto, o que vi durante o dia.
- **Agenda**: lembretes das minhas tarefas e eventos do Google.

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
- [ ] Fase 4 — Memória do dia: onde deixei chaves/carro/moto
- [ ] Fase 5 — Tarefas e eventos do Google (lembretes)
- [ ] Fase 6 — Tethering via Galaxy Watch 7 Pro (rede própria)

## Status

**Fase 0 — esqueleto do app.** Repositório privado.

Nome oficial decidido em 15/09/2026: **Live Companion** (ex-RokidLive). Spec oficial de produto em [docs/live-companion-spec.md](docs/live-companion-spec.md).

## Relacionados

- [Vision](https://github.com/marcusalemao/Vision) (ex-facecontext) — emulador AR / reconhecimento facial.
- [rokid-livestream](https://github.com/marcusalemao/rokid-livestream) — experimento de streaming constante câmera → servidor → assistente.
