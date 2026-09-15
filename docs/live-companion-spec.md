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