# Rokid Vision Assistant — APK nativo (esqueleto v0.1)

App Android (Kotlin) que roda **no próprio Android dos Rokid Glasses**, dando visão contínua e memória episódica ao Beto. Espelhamento por cabo foi descartado (os Rokid Glasses não têm entrada de vídeo — só os Rokid Max têm).

## Estado (14/09/2026)

- ✅ **CxrFrameSource IMPLEMENTADO (14/09)** — porta do GlassesCameraManager (v16, validado em produção nos óculos, extraído por descompilação do APK). Descoberta chave: a câmera dos Rokid Glasses abre via **Camera2 API direto** — nenhum AAR proprietário necessário.
- ⚠️ Ainda não compilado/testado — buildar no Android Studio e validar nos óculos.

## Arquitetura

```
hud/HudActivity.kt        HUD 640x480 verde fósforo, sem scroll, KeyEvents
vision/VisionService.kt  Foreground service: loop 1 FPS → diff → ingest
vision/FrameDiff.kt      Frame-differencing (porta do client HTML validado)
vision/FrameSource.kt     Interface — CameraX (dev) ↔ CXR SDK (óculos)
api/ApiClient.kt          POST nos endpoints Base44 (todos já no ar)
```

## Endpoints (backend já validado 14/09)

| Endpoint | Função |
|---|---|
| `rvFrameIngest` | frame 640x480 JPEG b64 + snapshot 160x120 + GPS → Gemini → EpisodicMemory |
| `rvAskMemory` | "onde deixei a chave?" → resposta curta + HUDNotification |
| `rvHudFeed` | poll de notificações pro HUD |

Sem auth de header (CORS liberado); chave Gemini fica server-side.

## Fluxo

1. `HudActivity` abre → starta `VisionService` (foreground, tipo camera)
2. Loop 1 FPS: frame → **diff local** (cena estática não sai do aparelho) → gate 4s (limite do backend) → POST
3. `hud_alert` da resposta → broadcast → HUD mostra por 4s
4. Poll `rvHudFeed` 5s pra notificações persistidas (perguntas respondidas etc.)

## Interação nos óculos (por definição do projeto)

- **KeyEvents apenas** — sem touch. Mapear keycodes reais em `HudActivity.onKeyDown`
- HUD 640x480, verde fósforo `#00FF66` sobre preto, sem scroll (trunca 180 chars)
- Respostas curtas; detalhe longo vai pro WhatsApp (fluxo óculos padrão)

## Próximos passos

1. Plugar CXR SDK (`app/libs/cxr-sdk.aar` → `CxrFrameSource.kt`) — referência: RokidPhone
2. Mapear keycodes físicos dos óculos no `onKeyDown`
3. Wake word "oi Beto" (offline) — módulo próprio depois da captura funcionando
4. Áudio: chunks → STT → `audio_transcript_chunk` (campo já aceito pelo ingest)
