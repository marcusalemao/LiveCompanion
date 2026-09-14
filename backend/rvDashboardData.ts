/**
 * RokidLive — Dados do Dashboard Companion (smartphone).
 *
 * POST {} → { memories, people, skills, notifications, usage }
 * Alimenta a rota /dashboard: memórias de objetos, linha do tempo de
 * pessoas, gerenciador de skills, status e consumo do mês.
 */

const CORS: Record<string, string> = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, api_key, x-api-key',
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...CORS },
  });
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: CORS });

  try {
    const { createClientFromRequest } = await import('npm:@base44/sdk@0.8.31');
    const base44 = createClientFromRequest(req);
    const db = base44.asServiceRole.entities;

    const [memories, people, skills, notifications, usage] = await Promise.all([
      db.EpisodicMemory.list('-created_date', 20),
      db.PersonInteraction.list('-created_date', 20),
      db.DynamicSkill.list('-created_date', 100),
      db.HUDNotification.list('-created_date', 20),
      db.AIUsageLog.list('-created_date', 500),
    ]);

    // resumo de consumo do mês (budget R$100)
    const monthKey = new Date().toISOString().slice(0, 7);
    const monthLogs = (usage ?? []).filter((u: any) => u.month_key === monthKey);
    const cost_brl = monthLogs.reduce((acc: number, u: any) => acc + (u.cost_brl ?? 0), 0);

    return json({
      ok: true,
      memories: memories ?? [],
      people: people ?? [],
      skills: skills ?? [],
      notifications: notifications ?? [],
      usage: {
        month_key: monthKey,
        calls: monthLogs.length,
        cost_brl: Math.round(cost_brl * 100) / 100,
        budget_brl: 100,
        pct: Math.round((cost_brl / 100) * 100),
      },
    });
  } catch (e) {
    return json({ ok: false, error: String(e) }, 500);
  }
});
