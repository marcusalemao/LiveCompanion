# Arquitetura — Live Companion

> Versão 1.1 — pipeline em produção (set/2026). A spec de produto completa está em [SPEC.md](SPEC.md).

## 1. Componentes

### 1.1 Client HUD (óculos / browser / WebView)
- `hud-bridge.html` — HTML5/JS vanilla: `getUserMedia` (câmera traseira 640x480), amostragem 1 FPS,
  frame-differencing client-side (descarte de cena estática), GPS acoplado a cada frame.
- Transporte: **HTTP POST** (sem WebSocket — backend Base44 é HTTP).
- Design system micro-OLED: fundo #000000, verde fósforo #00FF66, cards 1-2 linhas, bordas 1px.
- No app nativo Kotlin: mesmas regras, mas captura via CXR SDK e controle 100% por KeyEvents.

### 1.2 Backend (Superagent Beto — Base44)
Functions públicas em `https://base44.app/api/apps/6a11083db49430b410a8c066/functions/`:

| Function | Papel |
|---|---|
| `rvFrameIngest` | Ingestão do frame: frame-diff server-side → Gemini 2.5 Flash (System Prompt episódico + DynamicSkills ativas) → persistência |
| `rvAskMemory` | Q&A "onde deixei X?" → último EpisodicMemory → resposta curta + HUDNotification |
| `rvLearnSkill` | "Aprenda a [instrução]" → cria DynamicSkill com schema de extração |
| `rvHudFeed` | Poll de HUDNotifications pendentes (marca como exibidas) |
| `rvDashboardData` | Feed completo do dashboard (memórias, pessoas, skills, custo do mês) |

### 1.3 IA
- **Gemini 2.5 Flash** (chave própria do Marcus) — visão multimodal do frame + estruturação de skills.
- Custo logado por chamada em `AIUsageLog` (cost_brl, month_key) — budget R$100/mês.
- **Beto (Superagent)** — memória persistente, integrações (Calendar/Tasks), voz e WhatsApp.

## 2. Fluxo de dados

1. Client captura 1 FPS; se a cena mudou (diff > threshold), POST `rvFrameIngest` com frame JPEG 60% + GPS + timestamp.
2. Server: frame-diff duplicado (hash + rate-limit 4s) → chama Gemini com System Prompt episódico e skills ativas.
3. Detecções → EpisodicMemory (objetos pousados/estacionados), PersonInteraction (pessoas), HUDNotification (alertas de skill).
4. Client faz polling de `rvHudFeed` a cada 5s → projeta alertas no HUD.
5. "Onde deixei minha chave?" → `rvAskMemory` → resposta falada curta + card no HUD.

## 3. Decisões

- **HTTP POST em vez de WebSocket**: functions Base44 são HTTP; frame-diff duplo compensa o custo de polling.
- **Frame-diff em dois níveis**: client (economia de bateria/rede) + server (proteção do budget Gemini).
- **Rate-limit**: máx. 1 análise Gemini/4s por instância — a ~R$0,003/análise, uso contínuo ≈ R$40-60/mês dentro do budget.
- **Snapshot**: frame guardado como data URL no EpisodicMemory (renderização futura com dithering 1-bit no HUD).

## 4. Próximas fases

- Dashboard Companion (`/dashboard`) consumindo `rvDashboardData`.
- Workflow agendado no Beto: Google Calendar/Tasks → alertas HUDNotification (15 min antes de eventos).
- App nativo Kotlin (CXR SDK + KeyEvents + wake word "oi Beto") substituindo o client HTML.
- Tethering Galaxy Watch 7 Pro (rede independente do celular).
