[🇬🇧 English](#lc-spec-en) | [🇧🇷 Português](#lc-spec-pt-br)

<a id="lc-spec-en"></a>
# Live Companion Spec (English)

Update the full-stack application to the official name "Live Companion".
The app acts as a continuous, modular multimodal assistant for the Rokid Glasses (via HUD Mode) and is controlled through the Dashboard Mobile on the smartphone.

### 1. DATA MODEL (ENTITIES)

* **EpisodicMemory:**
  - id (UUID, PK)
  - item_name (string: e.g. "keys", "motorcycle", "wallet", "documents")
  - category (enum: "objeto_pessoal", "veiculo", "documento", "lugar")
  - location_description (text: detailed spatial landmark)
  - audio_context (text, optional: conversation/audio summary captured at the moment)
  - coordinates_lat (float, optional)
  - coordinates_lng (float, optional)
  - snapshot_url (image/file: monochrome frame compressed with 1-bit dithering)
  - captured_at (timestamp)

* **PersonInteraction:**
  - id (UUID, PK)
  - temp_identifier (string: e.g. "Person 1")
  - assigned_name (string, optional: e.g. "Carlos")
  - visual_anchors (text: clothing, accessories, transient physical traits)
  - conversation_summary (text: agreements, topics covered and pending items)
  - start_time (timestamp)
  - end_time (timestamp)

* **DynamicSkill:**
  - id (UUID, PK)
  - skill_name (string)
  - trigger_instruction (text: free-form audio trigger or visual detection)
  - extraction_schema (json: data structure to be extracted from the scene or audio)
  - action_type (string: e.g. "salvar_nota", "alerta_hud", "adicionar_tarefa")
  - is_active (boolean, default true)

* **HUDNotification:**
  - id (UUID, PK)
  - title (string)
  - content_type (enum: "live_dialogue", "memory_card", "line_art_table", "task_item")
  - payload (json: structured data for optical rendering)
  - displayed (boolean, default false)
  - created_at (timestamp)

### Addition (Sep 15, 2026, at Marcus's request): memory without facial recognition

The people pipeline must NOT depend on facial recognition to work. Mandatory fallback in `PersonInteraction`:

- `temp_identifier`: "Pessoa 1", "Pessoa 2"… numbered per session.
- `visual_anchors`: transient traits — "red coat, round glasses, black backpack".
- Location + time window in the summary: "11:43, Av. Paulista" (`start_time`/`end_time` + session location in `audio_context` or description).
- `conversation_summary`: topics, agreements and pending items extracted from audio.
- If facial recognition (Vision repo) identifies the person later, the `assigned_name` field is filled in and the interaction is linked to the permanent profile.

---

[🇬🇧 English](#lc-spec-en) | [🇧🇷 Português](#lc-spec-pt-br)

<a id="lc-spec-pt-br"></a>
# Spec Live Companion (Português)

Atualize a aplicação full-stack para o nome oficial "Live Companion".
O aplicativo atua como um assistente multimodal contínuo e modular para os óculos Rokid Glasses (via HUD Mode) e controle via Dashboard Mobile no smartphone.

### 1. MODELO DE DADOS (ENTIDADES)

* **EpisodicMemory:**
  - id (UUID, PK)
  - item_name (string: ex. "chaves", "motocicleta", "carteira", "documentos")
  - category (enum: "objeto_pessoal", "veiculo", "documento", "lugar")
  - location_description (text: ponto de referência espacial detalhado)
  - audio_context (text, opcional: resumo de conversa/áudio captado no momento)
  - coordinates_lat (float, opcional)
  - coordinates_lng (float, opcional)
  - snapshot_url (imagem/arquivo: frame monocromático comprimido em 1-bit dithering)
  - captured_at (timestamp)

* **PersonInteraction:**
  - id (UUID, PK)
  - temp_identifier (string: ex. "Pessoa 1")
  - assigned_name (string, opcional: ex. "Carlos")
  - visual_anchors (text: roupas, acessórios, características físicas transitórias)
  - conversation_summary (text: acordos, tópicos falados e pendências)
  - start_time (timestamp)
  - end_time (timestamp)

* **DynamicSkill:**
  - id (UUID, PK)
  - skill_name (string)
  - trigger_instruction (text: gatilho de áudio livre ou detecção visual)
  - extraction_schema (json: estrutura de dados a ser extraída da cena ou do áudio)
  - action_type (string: ex. "salvar_nota", "alerta_hud", "adicionar_tarefa")
  - is_active (boolean, default true)

* **HUDNotification:**
  - id (UUID, PK)
  - title (string)
  - content_type (enum: "live_dialogue", "memory_card", "line_art_table", "task_item")
  - payload (json: dados estruturados para renderização ótica)
  - displayed (boolean, default false)
  - created_at (timestamp)

### Adição (15/09/2026, a pedido do Marcus): memória sem reconhecimento facial

O pipeline de pessoas NÃO deve depender de reconhecimento facial para funcionar. Fallback obrigatório em `PersonInteraction`:

- `temp_identifier`: "Pessoa 1", "Pessoa 2"… numeradas por sessão.
- `visual_anchors`: características transitórias — "casaco vermelho, óculos redondo, mochila preta".
- Local + janela de tempo no resumo: "11:43, Av. Paulista" (`start_time`/`end_time` + localização da sessão em `audio_context` ou descrição).
- `conversation_summary`: tópicos, acordos e pendências extraídos do áudio.
- Se o reconhecimento facial (repo Vision) identificar a pessoa depois, o campo `assigned_name` é preenchido e a interação é ligada ao perfil permanente.
