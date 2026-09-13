import { useState, useEffect, useRef } from "react";
import ChatLayout from "../ChatLayout/ChatLayout";
import ChatOpenButton from "../ChatButtons/ChatOpenButton";
import { initSession, sendMessage, isManagerReply, type ChatAttachment } from "../../api/chatApi";
import { thinkingLabelForState } from "../ThinkingIndicator/ThinkingIndicator";

type ChatModes = 'closed' | 'floating' | 'full';

interface MessageInfo {
  id: string;
  text: string;
  messageType: 'user' | 'assistant' | 'sys-info' | 'error' | 'thinking';
  createdAt: number;
  attachments?: ChatAttachment[];
}

interface StoredChat {
  id: string;
  sid: string | null;
  title: string;
  messages: MessageInfo[];
  createdAt: number;
  updatedAt: number;
}

const WELCOME_TEXT = 'Здравствуйте!\nЯ ваш персональный консультант по недвижимости. Задайте любой вопрос — я помогу подобрать квартиру, расскажу о проектах и условиях покупки.';
const SID_KEY = 'dsk_sid';
const CHATS_KEY = 'dsk_chats_v1';
const ACTIVE_KEY = 'dsk_active_id_v1';
const MAX_CHATS = 20;
const MAX_MSGS = 100;

function makeWelcome(): MessageInfo {
  return { id: 'welcome', text: WELCOME_TEXT, messageType: 'assistant', createdAt: Date.now() };
}

function makeTitle(text: string): string {
  const t = text.trim().replace(/\s+/g, ' ');
  if (!t) return 'Новый чат';
  return t.length > 32 ? t.slice(0, 32) + '…' : t;
}

/** Только complex_image, макс. 3 — остальное (plan_image и пр.) фронт игнорирует. */
function pickComplexImages(list: ChatAttachment[] | undefined): ChatAttachment[] {
  if (!Array.isArray(list)) return [];
  return list.filter((a) => a && a.type === 'complex_image' && typeof a.url === 'string' && a.url.startsWith('http')).slice(0, 3);
}

/** Длинное КП и таблицы печатаем сразу — typewriter на них дёргается. */
function shouldPrintInstantly(full: string): boolean {
  return full.length > 800 || full.includes('|');
}

function trimMessages(msgs: MessageInfo[]): MessageInfo[] {
  if (msgs.length <= MAX_MSGS) return msgs;
  const welcome = msgs.find((m) => m.id === 'welcome');
  const rest = msgs.filter((m) => m.id !== 'welcome').slice(-(MAX_MSGS - 1));
  return welcome ? [welcome, ...rest] : msgs.slice(-MAX_MSGS);
}

function loadChats(): StoredChat[] {
  try {
    const raw = localStorage.getItem(CHATS_KEY);
    if (!raw) return [];
    const arr = JSON.parse(raw) as StoredChat[];
    if (!Array.isArray(arr)) return [];
    return arr.slice(0, MAX_CHATS);
  } catch { return []; }
}

function ChatWidget() {
  const [mode, setMode] = useState<ChatModes>('closed');
  const [isLoading, setIsLoading] = useState(false);
  const [isThinking, setIsThinking] = useState(false);
  const [messages, setMessages] = useState<MessageInfo[]>([makeWelcome()]);
  const [chats, setChats] = useState<StoredChat[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  // Последний известный state бэка — по нему подпись думанья (SHOWING_LIST/OFFER_READY/TO_MANAGER).
  const [lastBackendState, setLastBackendState] = useState<string | null>(null);

  const sidRef = useRef<string | null>(null);
  const messagesRef = useRef<MessageInfo[]>([]);
  const activeIdRef = useRef<string | null>(null);
  const chatsRef = useRef<StoredChat[]>([]);

  useEffect(() => { messagesRef.current = messages; }, [messages]);
  useEffect(() => { activeIdRef.current = activeId; }, [activeId]);
  useEffect(() => { chatsRef.current = chats; }, [chats]);

  useEffect(() => {
    const stored = loadChats();
    setChats(stored);
    try {
      const savedActive = localStorage.getItem(ACTIVE_KEY);
      const found = stored.find((c) => c.id === savedActive);
      if (found) {
        setActiveId(found.id);
        setMessages(found.messages.length ? found.messages : [makeWelcome()]);
        sidRef.current = found.sid;
        try {
          if (found.sid) localStorage.setItem(SID_KEY, found.sid);
        } catch { /* ignore */ }
        return;
      }
      if (stored.length) {
        const fresh = [...stored].sort((a, b) => b.updatedAt - a.updatedAt)[0];
        setActiveId(fresh.id);
        setMessages(fresh.messages);
        sidRef.current = fresh.sid;
        return;
      }
    } catch { /* ignore */ }
    try {
      sidRef.current = localStorage.getItem(SID_KEY);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => {
    try {
      localStorage.setItem(CHATS_KEY, JSON.stringify(chats.slice(0, MAX_CHATS)));
    } catch { /* ignore */ }
  }, [chats]);

  useEffect(() => {
    try {
      if (activeId) localStorage.setItem(ACTIVE_KEY, activeId);
      else localStorage.removeItem(ACTIVE_KEY);
    } catch { /* ignore */ }
  }, [activeId]);

  useEffect(() => {
    window.parent.postMessage({ type: 'CHAT_MODE_CHANGE', mode }, '*');
  }, [mode]);

  // Esc закрывает виджет из floating/full
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setMode('closed');
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  async function ensureSid(): Promise<string> {
    if (sidRef.current) return sidRef.current;
    const sid = await initSession();
    sidRef.current = sid;
    try { localStorage.setItem(SID_KEY, sid); } catch { /* ignore */ }
    return sid;
  }

  function clearSid() {
    sidRef.current = null;
    try { localStorage.removeItem(SID_KEY); } catch { /* ignore */ }
  }

  // Открытие из closed: продолжаем текущий чат, новый — только кнопкой «+» в меню
  function handleOpenFresh(next: ChatModes) {
    setMode(next);
  }

  function syncActiveToStore(finalMessages: MessageInfo[], sid: string | null, firstUserText?: string) {
    const aid = activeIdRef.current;
    const now = Date.now();
    if (aid === null) return null;
    const trimmed = trimMessages(finalMessages);
    setChats((prev) => {
      const next = prev.map((c) =>
        c.id === aid
          ? {
              ...c,
              sid,
              messages: trimmed,
              updatedAt: now,
              title: c.title === 'Новый чат' && firstUserText ? makeTitle(firstUserText) : c.title,
            }
          : c
      );
      return [...next].sort((a, b) => b.updatedAt - a.updatedAt).slice(0, MAX_CHATS);
    });
    return aid;
  }

  function createStoredChatFromDraft(finalMessages: MessageInfo[], sid: string, firstUserText: string) {
    const now = Date.now();
    const id = crypto.randomUUID();
    const trimmed = trimMessages(finalMessages);
    const chat: StoredChat = {
      id,
      sid,
      title: makeTitle(firstUserText),
      messages: trimmed,
      createdAt: now,
      updatedAt: now,
    };
    setChats((prev) => [chat, ...prev].slice(0, MAX_CHATS));
    setActiveId(id);
    try { localStorage.setItem(ACTIVE_KEY, id); } catch { /* ignore */ }
  }

  function setTextInstant(id: string, full: string) {
    setMessages((prev) => prev.map((m) => (m.id === id ? { ...m, text: full } : m)));
    return Promise.resolve();
  }

  function typewriter(id: string, full: string) {
    return new Promise<void>((resolve) => {
      if (!full) {
        setMessages((prev) => prev.map((m) => (m.id === id ? { ...m, text: '' } : m)));
        resolve();
        return;
      }
      // КП и таблицы — сразу, без эффекта печати
      if (shouldPrintInstantly(full)) {
        setTextInstant(id, full);
        resolve();
        return;
      }
      let i = 0;
      const step = Math.max(2, Math.ceil(full.length / 120));
      i += step;
      setMessages((prev) => prev.map((m) => (m.id === id ? { ...m, text: full.slice(0, i) } : m)));
      if (i >= full.length) { resolve(); return; }
      const timer = setInterval(() => {
        i += step;
        const slice = full.slice(0, i);
        setMessages((prev) => prev.map((m) => (m.id === id ? { ...m, text: slice } : m)));
        if (i >= full.length) { clearInterval(timer); resolve(); }
      }, 15);
    });
  }

  function mapError(status: number, msg: string, sid: string | null): string {
    if (status === 400) return `Неверный запрос: ${msg}`;
    if (status === 404 || status === 409) return 'Сессия не найдена. Начинаю новый чат, отправьте сообщение еще раз.';
    if (status === 429) return 'Превышен лимит запросов. Попробуйте чуть позже или попросите менеджера.';
    if (status >= 500) return `Сервер временно недоступен${sid ? ` (сессия ${sid.slice(0, 8)})` : ''}. Попробуйте позже.`;
    return `Ошибка сети: ${msg}`;
  }

  async function doRequest(userText: string) {
    const assistantId = crypto.randomUUID();
    const wasDraft = activeIdRef.current === null;
    setIsLoading(true);
    setIsThinking(true);
    setMessages((prev) => [...prev, { id: assistantId, text: '', messageType: 'assistant', createdAt: Date.now() }]);

    try {
      const sid = await ensureSid();
      const res = await sendMessage(sid, userText);
      setIsThinking(false);
      if (res.state) setLastBackendState(res.state);

      if (res.sessionId && res.sessionId !== sid) {
        sidRef.current = res.sessionId;
        try { localStorage.setItem(SID_KEY, res.sessionId); } catch { /* ignore */ }
      }
      const finalSid = sidRef.current;

      // Картинки ЖК — на сообщение ассистента (неизвестные типы уже отрезаны)
      const imgs = pickComplexImages(res.attachments);
      if (imgs.length) {
        setMessages((prev) => prev.map((m) => (m.id === assistantId ? { ...m, attachments: imgs } : m)));
      }

      await typewriter(assistantId, res.reply);

      // Флаг бэка — основной путь; эвристика по тексту — только fallback для старого бэка
      const transferred = res.transferredToManager === true || (res.transferredToManager == null && isManagerReply(res.reply));
      let withSys = messagesRef.current;
      if (transferred) {
        const sys: MessageInfo = { id: crypto.randomUUID(), text: 'Диалог передан менеджеру. Специалист свяжется с вами.', messageType: 'sys-info', createdAt: Date.now() };
        withSys = [...messagesRef.current, sys];
        setMessages(withSys);
      }

      const finalTrimmed = trimMessages(withSys);

      if (wasDraft) {
        createStoredChatFromDraft(finalTrimmed, finalSid ?? sid, userText);
        if (transferred) clearSid();
      } else {
        syncActiveToStore(finalTrimmed, finalSid, userText);
        if (transferred) clearSid();
      }
    } catch (e: any) {
      setIsThinking(false);
      const status = e?.status ?? 0;
      const sid = sidRef.current;
      if (status === 404 || status === 409) clearSid();
      setMessages((prev) =>
        prev.map((m) =>
          m.id === assistantId
            ? { ...m, messageType: 'error', text: mapError(status, e?.message ?? 'fetch failed', sid) }
            : m
        )
      );
      if (!wasDraft) {
        setTimeout(() => {
          syncActiveToStore(messagesRef.current, sidRef.current, userText);
        }, 0);
      }
    } finally {
      setIsLoading(false);
      setIsThinking(false);
    }
  }

  async function handleSend(inputText: string) {
    if (isLoading) return;
    const clean = inputText.trim();
    if (!clean) return;
    setMessages((prev) => [...prev, { id: crypto.randomUUID(), text: clean, messageType: 'user', createdAt: Date.now() }]);
    await doRequest(clean);
  }

  async function handleEdit(id: string, newText: string) {
    if (isLoading) return;
    const clean = newText.trim();
    if (!clean) return;
    setMessages((prev) => {
      const idx = prev.findIndex((m) => m.id === id);
      if (idx === -1) return prev;
      const updated: MessageInfo = { ...prev[idx], text: clean, createdAt: Date.now() };
      return [...prev.slice(0, idx), updated];
    });
    await doRequest(clean);
  }

  async function handleRegenerate() {
    if (isLoading) return;
    const lastUser = [...messages].reverse().find((m) => m.messageType === 'user');
    if (!lastUser) return;
    const lastId = lastUser.id;
    const lastText = lastUser.text;
    setMessages((prev) => {
      const idx = prev.findIndex((m) => m.id === lastId);
      if (idx === -1) return prev;
      return prev.slice(0, idx + 1);
    });
    await doRequest(lastText);
  }

  function handleSelectChat(id: string) {
    if (isLoading || id === activeIdRef.current) return;
    const target = chatsRef.current.find((c) => c.id === id);
    if (!target) return;
    setActiveId(target.id);
    setMessages(target.messages.length ? target.messages : [makeWelcome()]);
    sidRef.current = target.sid;
    try {
      if (target.sid) localStorage.setItem(SID_KEY, target.sid);
      else localStorage.removeItem(SID_KEY);
    } catch { /* ignore */ }
  }

  function handleDeleteChat(id: string) {
    if (isLoading && id === activeIdRef.current) return;
    const remaining = chatsRef.current.filter((c) => c.id !== id);
    setChats(remaining.slice(0, MAX_CHATS));
    if (id !== activeIdRef.current) return;
    if (remaining.length) {
      const fresh = [...remaining].sort((a, b) => b.updatedAt - a.updatedAt)[0];
      setActiveId(fresh.id);
      setMessages(fresh.messages.length ? fresh.messages : [makeWelcome()]);
      sidRef.current = fresh.sid;
      try {
        if (fresh.sid) localStorage.setItem(SID_KEY, fresh.sid);
        else localStorage.removeItem(SID_KEY);
      } catch { /* ignore */ }
    } else {
      setActiveId(null);
      setMessages([makeWelcome()]);
      clearSid();
    }
  }

  async function handleNewChat() {
    if (isLoading) return;
    if (activeIdRef.current === null && messagesRef.current.length <= 1) return;
    setActiveId(null);
    setMessages([makeWelcome()]);
    clearSid();
    setLastBackendState(null);
  }

  return (
    <>
      <div style={{ display: mode === 'closed' ? 'none' : 'contents' }}>
        <ChatLayout
          mode={mode}
          onModeChange={setMode}
          messages={messages}
          chats={chats}
          activeChatId={activeId}
          isLoading={isLoading}
          isThinking={isThinking}
          thinkingLabel={thinkingLabelForState(lastBackendState)}
          onSendMessage={handleSend}
          onEdit={handleEdit}
          onRegenerate={handleRegenerate}
          onNewChat={handleNewChat}
          onSelectChat={handleSelectChat}
          onDeleteChat={handleDeleteChat}
        />
      </div>
      {mode === 'closed' && <ChatOpenButton onModeChange={handleOpenFresh} />}
    </>
  );
}

export default ChatWidget;
