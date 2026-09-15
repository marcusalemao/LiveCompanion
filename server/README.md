[🇬🇧 English](#gateway-en) | [🇧🇷 Português](#gateway-pt-br)

<a id="gateway-en"></a>
# Rokid Vision Gateway — local/offline mode (FUTURE) (English)

FastAPI/WebSocket gateway written by Gemini (Sep 14, 2026), reviewed by Beto.
**Status: NOT in use.** Today's production pipeline runs serverless on the
Base44 backend functions (`backend/rvFrameIngest.ts` and siblings), validated
end to end. This server is plan B: run on a VPS (Oracle Cloud, when it exists)
as a local/offline mode, or for offline development.

## What is already good

- `ProcessingLock`: discards frames if the previous inference is still running
  (equivalent to rvFrameIngest's 4s rate-limit).
- `alerta_hud` pushed instantly over WebSocket — instant confirmation
  on the glasses. (Idea already adopted in production: rvFrameIngest v1.0.4 returns
  `hud_alert` in the POST and the client shows the green overlay.)
- Lean prompt with strict JSON schema.

## Mandatory fixes before running (marked with `# FIX:` in the code)

1. **Base44 endpoints**: call the real backend functions at
   `https://base44.app/api/apps/<APP_ID>/functions/rvFrameIngest`, not
   `/episodic_memories` (nonexistent endpoint).
2. **Auth**: the project's functions are called with a simple POST
   (no `Authorization`/`api_key` in internal prod). NEVER send
   `Authorization` + `api_key` together — Base44 returns 403 (bug already
   faced in RokidPhone v14).
3. **Budget**: log cost in `AIUsageLog` like rvFrameIngest does
   (R$100/month budget).
4. **DynamicSkills**: attach active skills to the vision prompt.
5. **Frame-diff**: hash the frame to skip static scenes (rvFrameIngest
   uses djb2 by sampling).
6. **Transport**: needs HTTPS/WSS (uvicorn behind Caddy, or a secure dev tunnel) — the HUD client only opens the camera in a secure context.

## Run (when the time comes)

```bash
pip install fastapi uvicorn httpx google-genai
export GEMINI_API_KEY=...
uvicorn gateway:app --host 0.0.0.0 --port 8000
```

---

[🇬🇧 English](#gateway-en) | [🇧🇷 Português](#gateway-pt-br)

<a id="gateway-pt-br"></a>
# Rokid Vision Gateway — modo local/offline (FUTURO) (Português)

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
   túnel seguro em dev) — o client HUD só abre câmera em contexto seguro.

## Rodar (quando chegar a hora)

```bash
pip install fastapi uvicorn httpx google-genai
export GEMINI_API_KEY=...
uvicorn gateway:app --host 0.0.0.0 --port 8000
```
