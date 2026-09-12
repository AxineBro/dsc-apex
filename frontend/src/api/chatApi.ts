// База пустая = относительный путь.
// dev:  :5173/api -> vite proxy -> :8080
// prod: :80/api   -> nginx -> backend:8080
const API_BASE = '';

export interface ChatAttachment {
  type: string;
  url: string;
  title?: string;
}

export interface ChatOk {
  reply: string;
  sessionId: string;
  /** Текущий SessionState строкой: NEW/AWAITING_INPUT/SHOWING_LIST/OFFER_READY/TO_MANAGER/FINISHED. */
  state?: string;
  /** true только в том ответе, где произошёл перевод менеджеру. */
  transferredToManager?: boolean;
  /** Код причины перевода: user_requested/booked/no_options/... */
  managerReason?: string | null;
  /** Вложения (complex_image — картинки ЖК, макс. 3). */
  attachments?: ChatAttachment[];
}

export interface ChatApiError {
  status: number;
  message: string;
  sessionId?: string;
}

export async function initSession(): Promise<string> {
  const r = await fetch(`${API_BASE}/api/v1/chat/init`);
  if (!r.ok) {
    throw { status: r.status, message: `init failed: ${r.status}` } as ChatApiError;
  }
  const j = await r.json();
  return j.sessionId as string;
}

export async function sendMessage(sessionId: string | null, message: string): Promise<ChatOk> {
  const clean = message.trim().slice(0, 2000);
  if (!clean) {
    throw { status: 400, message: 'Пустое сообщение' } as ChatApiError;
  }

  const r = await fetch(`${API_BASE}/api/v1/chat/message`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(sessionId ? { 'X-Session-Id': sessionId } : {}),
    },
    body: JSON.stringify({ message: clean }),
  });

  if (!r.ok) {
    let body: any = {};
    try {
      body = await r.json();
    } catch { /* ignore */ }
    throw {
      status: r.status,
      message: body.message || body.error || `HTTP ${r.status}`,
      sessionId,
    } as ChatApiError;
  }

  const j = await r.json();
  return {
    reply: j.reply ?? '',
    sessionId: j.sessionId,
    state: j.state,
    transferredToManager: j.transferredToManager,
    managerReason: j.managerReason,
    attachments: Array.isArray(j.attachments) ? j.attachments : [],
  };
}

// Fallback-эвристика менеджера для старых версий бэка без флага.
// Основной путь — res.transferredToManager, это только запасной.
export function isManagerReply(reply: string): boolean {
  const s = reply.toLowerCase();
  return (
    s.includes('менеджер') ||
    s.includes('специалист свяжется') ||
    s.includes('соединяю со специалистом') ||
    s.includes('передаю менеджеру')
  );
}
