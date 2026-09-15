[🇬🇧 English](#apk-en) | [🇧🇷 Português](#apk-pt-br)

<a id="apk-en"></a>
# Rokid Vision Assistant — Native APK (skeleton v0.1) (English)

Android app (Kotlin) that runs **on the Rokid Glasses' own Android**, giving Beto continuous vision and episodic memory. Cable mirroring was discarded (the Rokid Glasses have no video input — only the Rokid Max do).

## Status (Sep 14, 2026)

- ✅ **CxrFrameSource IMPLEMENTED (Sep 14)** — port of GlassesCameraManager (v16, validated in production on the glasses, extracted by decompiling the APK). Key finding: the Rokid Glasses camera opens via the **Camera2 API directly** — no proprietary AAR needed.
- ⚠️ Not yet compiled/tested — build in Android Studio and validate on the glasses.

## Architecture

```
hud/HudActivity.kt        640x480 phosphor-green HUD, no scroll, KeyEvents
vision/VisionService.kt  Foreground service: 1 FPS loop → diff → ingest
vision/FrameDiff.kt      Frame-differencing (port of the validated HTML client)
vision/FrameSource.kt     Interface — CameraX (dev) ↔ CXR SDK (glasses)
api/ApiClient.kt          POST to the Base44 endpoints (all already live)
```

## Endpoints (backend validated Sep 14)

| Endpoint | Purpose |
|---|---|
| `rvFrameIngest` | 640x480 JPEG b64 frame + 160x120 snapshot + GPS → Gemini → EpisodicMemory |
| `rvAskMemory` | "where did I leave the key?" → short answer + HUDNotification |
| `rvHudFeed` | notifications poll for the HUD |

No header auth (CORS open); the Gemini key stays server-side.

## Flow

1. `HudActivity` opens → starts `VisionService` (foreground, camera type)
2. 1 FPS loop: frame → **local diff** (static scene never leaves the device) → 4s gate (backend limit) → POST
3. `hud_alert` from the response → broadcast → HUD shows it for 4s
4. Poll `rvHudFeed` every 5s for persisted notifications (answered questions etc.)

## Interaction on the glasses (by project definition)

- **KeyEvents only** — no touch. Map real keycodes in `HudActivity.onKeyDown`
- 640x480 HUD, phosphor green `#00FF66` on black, no scroll (truncates 180 chars)
- Short answers; long detail goes to WhatsApp (standard glasses flow)

## Next steps

1. Plug in the CXR SDK (`app/libs/cxr-sdk.aar` → `CxrFrameSource.kt`) — reference: RokidPhone
2. Map the glasses' physical keycodes in `onKeyDown`
3. "oi Beto" wake word (offline) — dedicated module after capture works
4. Audio: chunks → STT → `audio_transcript_chunk` (field already accepted by ingest)

---

[🇬🇧 English](#apk-en) | [🇧🇷 Português](#apk-pt-br)

<a id="apk-pt-br"></a>
# Rokid Vision Assistant — APK nativo (esqueleto v0.1) (Português)

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
