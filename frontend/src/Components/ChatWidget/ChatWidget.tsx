import { useState, useEffect, useRef } from "react";
import ChatLayout from "../ChatLayout/ChatLayout";
import ChatOpenButton from "../ChatButtons/ChatOpenButton";
import { initSession, sendMessage, isManagerReply } from "../../api/chatApi";

type ChatModes = 'closed' | 'floating' | 'full';

interface MessageInfo {
  id: string;
  text: string;
  messageType: 'user' | 'assistant' | 'sys-info' | 'error' | 'thinking';
  createdAt: number;
}

const WELCOME_TEXT = 'Здравствуйте!\nЯ ваш персональный консультант по недвижимости. Задайте любой вопрос — я помогу подобрать квартиру, расскажу о проектах и условиях покупки.';
const SID_KEY = 'dsk_sid';

function ChatWidget() {
  const [mode, setMode] = useState<ChatModes>('closed');
  const [isLoading, setIsLoading] = useState(false);
  const [isThinking, setIsThinking] = useState(false);
  const [messages, setMessages] = useState<MessageInfo[]>([
    { id: 'welcome', text: WELCOME_TEXT, messageType: 'assistant', createdAt: Date.now() },
  ]);
  const sidRef = useRef<string | null>(null);

  useEffect(() => {
    try {
      sidRef.current = localStorage.getItem(SID_KEY);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => {
    window.parent.postMessage({ type: 'CHAT_MODE_CHANGE', mode }, '*');
  }, [mode]);

  async function ensureSid(): Promise<string> {
    if (sidRef.current) return sidRef.current;
    const sid = await initSession();
    sidRef.current = sid;
    try {
      localStorage.setItem(SID_KEY, sid);
    } catch { /* ignore */ }
    return sid;
  }

  function clearSid() {
    sidRef.current = null;
    try {
      localStorage.removeItem(SID_KEY);
    } catch { /* ignore */ }
  }

  function typewriter(id: string, full: string) {
    return new Promise<void>((resolve) => {
      if (!full) {
        setMessages((prev) => prev.map((m) => (m.id === id ? { ...m, text: '' } : m)));
        resolve();
        return;
      }
      let i = 0;
      const step = Math.max(2, Math.ceil(full.length / 120));
      i += step;
      setMessages((prev) => prev.map((m) => (m.id === id ? { ...m, text: full.slice(0, i) } : m)));
      if (i >= full.length) {
        resolve();
        return;
      }
      const timer = setInterval(() => {
        i += step;
        const slice = full.slice(0, i);
        setMessages((prev) => prev.map((m) => (m.id === id ? { ...m, text: slice } : m)));
        if (i >= full.length) {
          clearInterval(timer);
          resolve();
        }
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
    setIsLoading(true);
    setIsThinking(true);
    setMessages((prev) => [...prev, { id: assistantId, text: '', messageType: 'assistant', createdAt: Date.now() }]);

    try {
      const sid = await ensureSid();
      const res = await sendMessage(sid, userText);
      setIsThinking(false);

      if (res.sessionId && res.sessionId !== sid) {
        sidRef.current = res.sessionId;
        try {
          localStorage.setItem(SID_KEY, res.sessionId);
        } catch { /* ignore */ }
      }

      await typewriter(assistantId, res.reply);

      if (isManagerReply(res.reply)) {
        setMessages((prev) => [
          ...prev,
          { id: crypto.randomUUID(), text: 'Диалог передан менеджеру. Специалист свяжется с вами.', messageType: 'sys-info', createdAt: Date.now() },
        ]);
        clearSid();
      }
    } catch (e: any) {
      setIsThinking(false);
      const status = e?.status ?? 0;
      const sid = sidRef.current;
      if (status === 404 || status === 409) {
        clearSid();
      }
      setMessages((prev) =>
        prev.map((m) =>
          m.id === assistantId
            ? { ...m, messageType: 'error', text: mapError(status, e?.message ?? 'fetch failed', sid) }
            : m
        )
      );
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

  async function handleNewChat() {
    clearSid();
    setIsLoading(false);
    setIsThinking(false);
    setMessages([{ id: 'welcome', text: WELCOME_TEXT, messageType: 'assistant', createdAt: Date.now() }]);
    try {
      const sid = await initSession();
      sidRef.current = sid;
      localStorage.setItem(SID_KEY, sid);
    } catch { /* ignore */ }
  }

  return (
    <>
      <div style={{ display: mode === 'closed' ? 'none' : 'contents' }}>
        <ChatLayout
          mode={mode}
          onModeChange={setMode}
          messages={messages}
          isLoading={isLoading}
          isThinking={isThinking}
          onSendMessage={handleSend}
          onEdit={handleEdit}
          onRegenerate={handleRegenerate}
          onNewChat={handleNewChat}
        />
      </div>
      {mode === 'closed' && <ChatOpenButton onModeChange={setMode} />}
    </>
  );
}

export default ChatWidget;