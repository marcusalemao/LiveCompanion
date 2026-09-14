"""
Rokid Vision Gateway — modo local/offline (FUTURO).

Escrito pelo Gemini (14/09/2026), avaliado pelo Beto. NÃO está em uso —
o pipeline de produção roda nas backend functions Base44 (rvFrameIngest
v1.0.4+). Este arquivo é o plano B para VPS/offline: ver server/README.md
para a lista completa de fixes obrigatórios (marcados abaixo com # FIX:).
"""

import os
import json
import base64
import asyncio
from typing import Optional
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
import httpx
from google import genai
from google.genai import types

app = FastAPI(title="Rokid Vision Gateway")

# Configurações de Ambiente
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "SUA_CHAVE_AQUI")
# FIX 1: o endpoint correto é a backend function, ex:
#   https://base44.app/api/apps/<APP_ID>/functions/rvFrameIngest
# FIX 2: POST simples, SEM header Authorization+api_key juntos (403 garantido)
BASE44_API_URL = os.getenv("BASE44_API_URL", "https://base44.app/api/apps/<APP_ID>/functions")

# Cliente Gemini Oficial
ai_client = genai.Client(api_key=GEMINI_API_KEY)
http_client = httpx.AsyncClient(timeout=10.0)

# Prompt de Extração Semântica para Memória Episódica
VISION_SYSTEM_INSTRUCTION = """
Você é o motor de visão computacional de um óculos AR inteligente (Rokid Glasses).
Analise o frame recebido a 1 FPS com extrema precisão visual e retorne ESTRITAMENTE um objeto JSON (sem markdown de bloco ```json).

Regras de Detecção:
1. "item_detectado": Identifique se o usuário acabou de guardar, soltar ou estacionar: chaves, moto, carro, carteira, fone ou capacete.
2. "pessoa_detectada": Se houver alguém conversando com o usuário, descreva âncoras visuais (cor da roupa, acessórios, contexto).
3. "relevancia": "alta" se registrou um evento de memória crítica (ex: largou a chave, estacionou a moto), senão "baixa".

Schema JSON esperado:
{
  "relevancia": "alta" | "baixa",
  "tipo": "objeto_pessoal" | "veiculo" | "interacao_humana" | "nenhum",
  "item": "nome do item ou null",
  "ponto_referencia": "descrição concisa e exata do local (ex: aparador preto ao lado do vaso) ou null",
  "detalhes_pessoa": "descrição visual da pessoa ou null",
  "alerta_hud": "texto curto em maiúsculas de até 5 palavras para exibir no óculos ou null"
}
"""

class ProcessingLock:
    """Evita sobrecarga descartando frames caso a análise anterior ainda esteja rodando."""
    def __init__(self):
        self.is_busy = False

processing_lock = ProcessingLock()

async def sync_with_base44(endpoint: str, data: dict):
    """Encaminha os dados estruturados para persistência no Base44.
    FIX 1: usar os nomes reais das functions: rvFrameIngest, rvHudFeed, rvAskMemory...
    FIX 3: o custo de cada análise deve ser logado em AIUsageLog (budget R$100/mês)."""
    try:
        response = await http_client.post(f"{BASE44_API_URL}/{endpoint}", json=data)
        response.raise_for_status()
    except Exception as e:
        print(f"[Base44 Sync Error] Falha ao enviar para {endpoint}: {e}")

async def analyze_frame_with_ai(image_bytes: bytes) -> Optional[dict]:
    """Envia o frame para o modelo multimodal extrair dados estruturados.
    FIX 4: anexar as DynamicSkills ativas ao prompt (como o rvFrameIngest faz).
    FIX 5: frame-diff por hash antes de chamar o Gemini (cenas estáticas não pagam)."""
    try:
        response = await asyncio.to_thread(
            ai_client.models.generate_content,
            model="gemini-2.5-flash",
            contents=[
                types.Part.from_bytes(data=image_bytes, mime_type="image/jpeg"),
                "Analise este frame segundo as instruções de sistema."
            ],
            config=types.GenerateContentConfig(
                system_instruction=VISION_SYSTEM_INSTRUCTION,
                response_mime_type="application/json",
                temperature=0.1
            )
        )
        return json.loads(response.text)
    except Exception as e:
        print(f"[AI Vision Error]: {e}")
        return None

@app.websocket("/api/stream/ws")
async def websocket_stream_endpoint(websocket: WebSocket):
    await websocket.accept()
    print("[WebSocket] Cliente Rokid conectado.")

    try:
        while True:
            raw_data = await websocket.receive_text()
            payload = json.loads(raw_data)
            msg_type = payload.get("type")

            # 1. Pipeline de Frame de Vídeo (1 FPS)
            if msg_type == "frame_ingest":
                if processing_lock.is_busy:
                    # Descarta o frame se a inferência anterior ainda não concluiu
                    continue

                processing_lock.is_busy = True
                asyncio.create_task(
                    handle_frame_processing(websocket, payload)
                )

            # 2. Pipeline de Áudio / Conversas
            elif msg_type == "audio_chunk":
                # Salva ou encaminha buffer de áudio para diarização/transcrição
                pass

    except WebSocketDisconnect:
        print("[WebSocket] Cliente Rokid desconectado.")
    except Exception as e:
        print(f"[WebSocket Error]: {e}")

async def handle_frame_processing(websocket: WebSocket, payload: dict):
    try:
        # Decodifica imagem Base64 vinda do canvas
        header, encoded = payload["frame_base64"].split(",", 1)
        image_bytes = base64.b64decode(encoded)

        # Inferência multimodal assíncrona
        analysis = await analyze_frame_with_ai(image_bytes)

        if analysis and analysis.get("relevancia") == "alta":
            # 1. Registra no banco Base44
            memory_entry = {
                "item_name": analysis.get("item"),
                "category": analysis.get("tipo"),
                "location_description": analysis.get("ponto_referencia"),
                "coordinates_lat": payload.get("lat"),
                "coordinates_lng": payload.get("lng"),
                "captured_at": payload.get("timestamp")
            }
            await sync_with_base44("rvFrameIngest", memory_entry)

            # 2. Projeta confirmação imediata no HUD do Rokid
            #    (ideia já adotada em produção: rvFrameIngest devolve hud_alert no POST)
            hud_text = analysis.get("alerta_hud") or f"SALVO: {analysis.get('item', '').upper()}"
            await websocket.send_text(json.dumps({
                "type": "hud_alert",
                "text": hud_text
            }))

    finally:
        processing_lock.is_busy = False

@app.on_event("shutdown")
async def shutdown_event():
    await http_client.aclose()
