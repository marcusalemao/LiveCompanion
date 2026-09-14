# Rokid Vision Gateway — modo local/offline (FUTURO)

Gateway FastAPI/WebSocket escrito pelo Gemini (14/09/2026), avaliado pelo Beto.
**Status: NÃO está em uso.** O pipeline de produção hoje roda serverless nas
backend functions do Base44 (`backend/rvFrameIngest.ts` e irmãs), validado
de ponta a ponta. Este servidor é o plano B: rodar numa VPS (Oracle Cloud,
quando existir) como modo local/offline, ou para desenvolvimento offline.

## O que já é bom

- `ProcessingLock`: descarta frames se a inferência anterior ainda rodar
  (equivalente ao rate-limit de 4s do rvFrameIngest).
- `alerta_hud` empurrado na hora pelo WebSocket — confirmação instantânea
  no óculos. (Ideia já adotada em produção: rvFrameIngest v1.0.4 devolve
  `hud_alert` no POST e o client mostra o overlay verde.)
- Prompt enxuto com schema JSON estrito.

## Fixes obrigatórios antes de rodar (anotados com `# FIX:` no código)

1. **Endpoints Base44**: chamar as backend functions reais em
   `https://base44.app/api/apps/<APP_ID>/functions/rvFrameIngest`, não
   `/episodic_memories` (endpoint inexistente).
2. **Auth**: as functions do projeto são chamadas com POST simples
   (sem `Authorization`/`api_key` em prod interno). NUNCA enviar
   `Authorization` + `api_key` juntos — o Base44 devolve 403 (bug já
   enfrentado no RokidPhone v14).
3. **Budget**: logar custo em `AIUsageLog` como o rvFrameIngest faz
   (budget R$100/mês).
4. **DynamicSkills**: anexar skills ativas ao prompt de visão.
5. **Frame-diff**: hash do frame pular cenas estáticas (rvFrameIngest
   usa djb2 por amostragem).
6. **Transporte**: precisa de HTTPS/WSS (uvicorn atrás de Caddy, ou
   ngrok em dev) — o client HUD só abre câmera em contexto seguro.

## Rodar (quando chegar a hora)

```bash
pip install fastapi uvicorn httpx google-genai
export GEMINI_API_KEY=...
uvicorn gateway:app --host 0.0.0.0 --port 8000
```
