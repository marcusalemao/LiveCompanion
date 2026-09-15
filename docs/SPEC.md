[🇬🇧 English](#spec-en) | [🇧🇷 Português](#spec-pt-br)

<a id="spec-en"></a>
# SPEC — Rokid Vision Assistant (v1, Sep 14, 2026)

Spec drafted with Gemini and validated by Marcus. This document is the product's source of truth.

## Vision

Full-stack "Rokid Vision Assistant" app: low-latency backend API, episodic memory module with multimodal AI, and two interfaces — **HUD Mode** (projection on Rokid glasses) and **Dashboard Mobile** (control on the smartphone).

## 1. Data model (entities)

| Entity | Fields | Purpose |
|---|---|---|
| **EpisodicMemory** | item_name, category (veiculo/objeto_pessoal/documento/outro), location_description, coordinates_lat/lng, snapshot_url (monochrome frame), captured_at | Where I left my keys, motorcycle, car, wallet |
| **PersonInteraction** | temp_identifier, assigned_name, visual_anchors (clothes/glasses/accessories), conversation_summary, start_time, end_time | Who interacted with me and about what |
| **DynamicSkill** | skill_name, trigger_instruction, extraction_schema (json), action_type (salvar_nota/alerta_hud/adicionar_tarefa), is_active | Voice-learned rules |
| **HUDNotification** | title, content_type (line_art_table/metric/alert/task_item), payload (json for HUD rendering), displayed, created_at | Rendering queue for the optical display |

## 2. Backend and vision pipeline (1 FPS)

1. **Frame ingest** (`POST frame_base64, timestamp, lat, lng, audio_transcript_chunk?`):
   - Frame-differencing to discard static-camera frames.
   - Multimodal model with episodic vision System Prompt: extracts parked/parked objects (location + landmarks in JSON) and interacting people (visual anchors + summary).
   - Auto-saves into EpisodicMemory / PersonInteraction.
2. **DynamicSkill Engine**: "Learn to [instruction]" command → AI creates a DynamicSkill entry and generates the visual extraction rule for upcoming frames.
3. **Voice Q&A**: "Where did I leave my key?" → looks up the latest EpisodicMemory for the item → short spoken answer + HUDNotification formatted for the optical display.
4. **Google Workspace**: syncs tasks and events; HUD alert 15 min before appointments or when critical tasks are pending.

## 3. HUD Mode interface (micro-OLED waveguide)

- **100% black** background (#000000 — physical transparency on the glasses). Text/icons in **phosphor green (#00FF66)** or pure white. No gradients or grays.
- Cards with **max 2 lines**, ultra-thin 1px borders.
- Hollow monochrome SVG icons (keys, motorcycles, alerts, people).
- Compact terminal/blueprint-style tables (max 2-3 columns).
- Reminder photos with high-contrast **1-bit monochrome dithering** filter.
- Glanceable layout: upper-right quadrant or lower-center.

## 4. Companion Dashboard (smartphone)

- Object memory list ("View last location" button).
- Timeline of people and conversation summaries.
- Skills manager (enable/disable/edit).
- Camera connection status + 1 FPS capture toggle.

---

## Implementation (spec → production mapping)

| Spec item | Implemented in |
|---|---|
| Entities | Superagent Beto backend (Base44): EpisodicMemory, PersonInteraction, DynamicSkill, HUDNotification |
| Frame ingest | Function `rvFrameIngest` (HTTP POST; double frame-diff: client + server) |
| DynamicSkill Engine | Function `rvLearnSkill` |
| Voice Q&A | Function `rvAskMemory` |
| HUD feed | Function `rvHudFeed` (poll) |
| Dashboard data | Function `rvDashboardData` |
| HUD Mode (web) | `hud-bridge.html` (client: getUserMedia 1 FPS + GPS + frame-diff) |
| Dashboard Mobile | Next phase (`/dashboard`) |
| Google Workspace | Next phase (scheduled workflow on Beto: Calendar/Tasks → HUDNotification) |

Functions base URL: `https://base44.app/api/apps/6a11083db49430b410a8c066/functions/<name>`

### Architecture adjustments vs. original spec
- **No WebSocket**: the Base44 backend exposes HTTP POST only; the client sends frames via POST and receives alerts via polling (5s). Frame-differencing happens TWICE (client saves network/battery; server protects the Gemini budget).
- **Budget**: every Gemini call is logged in AIUsageLog with cost in BRL (R$100/month cap).
- **Rate-limit**: at most 1 Gemini analysis every 4s per instance.

---

[🇬🇧 English](#spec-en) | [🇧🇷 Português](#spec-pt-br)

<a id="spec-pt-br"></a>
# SPEC — Rokid Vision Assistant (v1, 14/09/2026)

Spec elaborada com Gemini e validada por Marcus. Este documento é a fonte da verdade do produto.

## Visão

App full-stack "Rokid Vision Assistant": API de backend de baixa latência, módulo de memória episódica com IA multimodal, e duas interfaces — **HUD Mode** (projeção nos óculos Rokid) e **Dashboard Mobile** (controle no smartphone).

## 1. Modelo de dados (entidades)

| Entidade | Campos | Função |
|---|---|---|
| **EpisodicMemory** | item_name, category (veiculo/objeto_pessoal/documento/outro), location_description, coordinates_lat/lng, snapshot_url (frame monocromático), captured_at | Onde deixei chaves, moto, carro, carteira |
| **PersonInteraction** | temp_identifier, assigned_name, visual_anchors (roupa/óculos/acessórios), conversation_summary, start_time, end_time | Quem interagiu comigo e sobre o quê |
| **DynamicSkill** | skill_name, trigger_instruction, extraction_schema (json), action_type (salvar_nota/alerta_hud/adicionar_tarefa), is_active | Regras aprendidas por voz |
| **HUDNotification** | title, content_type (line_art_table/metric/alert/task_item), payload (json para render no HUD), displayed, created_at | Fila de renderização do display óptico |

## 2. Backend e pipeline de visão (1 FPS)

1. **Frame ingest** (`POST frame_base64, timestamp, lat, lng, audio_transcript_chunk?`):
   - Frame-Differencing para descartar frames de câmera estática.
   - Modelo multimodal com System Prompt de visão episódica: extrai objetos pousados/estacionados (local + pontos de referência em JSON) e pessoas interagindo (âncoras visuais + resumo).
   - Salva automaticamente em EpisodicMemory / PersonInteraction.
2. **DynamicSkill Engine**: comando "Aprenda a [instrução]" → IA cria entrada em DynamicSkill e gera a regra de extração visual para os próximos frames.
3. **Q&A por voz**: "Onde deixei a minha chave?" → busca o último EpisodicMemory do item → resposta falada curta + HUDNotification formatada para o display óptico.
4. **Google Workspace**: sincroniza tarefas e eventos; alerta no HUD 15 min antes de compromissos ou com tarefas críticas pendentes.

## 3. Interface HUD Mode (micro-OLED Waveguide)

- Fundo **100% preto** (#000000 — transparência física nos óculos). Texto/ícones em **verde fósforo (#00FF66)** ou branco puro. Sem gradientes ou cinzas.
- Cards de **máx. 2 linhas**, bordas ultrafinas 1px.
- Ícones vazados em SVG monocromático (chaves, motos, alertas, pessoas).
- Tabelas compactas estilo terminal/blueprint (máx. 2-3 colunas).
- Fotos de lembrete com filtro **dithering 1-bit monocromático** de alto contraste.
- Layout glanceable: quadrante superior direito ou inferior central.

## 4. Dashboard Companion (smartphone)

- Lista de memórias de objetos (botão "Ver última localização").
- Linha do tempo de pessoas e resumos de conversas.
- Gerenciador de Skills (ativar/desativar/editar).
- Status da conexão da câmera + alternador de captura 1 FPS.

---

## Implementação (mapeamento da spec → produção)

| Item da spec | Implementado em |
|---|---|
| Entidades | Backend do Superagent Beto (Base44): EpisodicMemory, PersonInteraction, DynamicSkill, HUDNotification |
| Frame ingest | Function `rvFrameIngest` (POST HTTP; frame-diff duplo: client + server) |
| DynamicSkill Engine | Function `rvLearnSkill` |
| Q&A por voz | Function `rvAskMemory` |
| Feed de HUD | Function `rvHudFeed` (poll) |
| Dashboard data | Function `rvDashboardData` |
| HUD Mode (web) | `hud-bridge.html` (client: getUserMedia 1 FPS + GPS + frame-diff) |
| Dashboard Mobile | Próxima fase (`/dashboard`) |
| Google Workspace | Próxima fase (workflow agendado no Beto: Calendar/Tasks → HUDNotification) |

URL base das functions: `https://base44.app/api/apps/6a11083db49430b410a8c066/functions/<nome>`

### Ajustes de arquitetura vs. spec original
- **Sem WebSocket**: o backend Base44 expõe HTTP POST apenas; o client envia frames via POST e recebe alertas via polling (5s). O frame-differencing é feito DUAS vezes (client economiza rede/bateria; server protege o budget Gemini).
- **Budget**: cada chamada Gemini é logada em AIUsageLog com custo em BRL (teto R$100/mês).
- **Rate-limit**: no máx. 1 análise Gemini a cada 4s por instância.
