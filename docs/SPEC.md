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
