/**
 * RokidLive — Serve o client HUD (hud-bridge.html) como página HTTPS.
 * v1.0.1 — GET busca o HTML do storage público do Base44 e devolve com
 * content-type text/html (o link de storage serve octet-stream/download).
 * Atualizar o client = re-upload do arquivo + trocar CLIENT_URL aqui.
 * v1.0.1: aponta para a versão PWA (manifest fullscreen + ícone + requestFullscreen).
 */

const CLIENT_URL = 'https://media.base44.com/files/public/6a11083db49430b410a8c066/ca5964521_hud-bridge.html';

Deno.serve(async (req) => {
  try {
    const res = await fetch(CLIENT_URL);
    if (!res.ok) {
      return new Response('client nao encontrado no storage', { status: 502 });
    }
    const html = await res.text();
    return new Response(html, {
      status: 200,
      headers: {
        'Content-Type': 'text/html; charset=utf-8',
        'Cache-Control': 'no-store',
      },
    });
  } catch (e) {
    return new Response('erro ao servir client: ' + String(e), { status: 500 });
  }
});
