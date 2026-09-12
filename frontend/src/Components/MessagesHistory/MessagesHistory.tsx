import { useEffect, useRef } from 'react';
import './MessagesHistory.css';
import Message from '../Messgae/Message';
import Suggestions from '../Suggestions/Suggestions';
import ThinkingIndicator from '../ThinkingIndicator/ThinkingIndicator';
import type { ChatAttachment } from '../../api/chatApi';

interface MessageInfo {
  id: string;
  text: string;
  messageType: 'user' | 'assistant' | 'sys-info' | 'error' | 'thinking';
  createdAt: number;
  attachments?: ChatAttachment[];
}

type Props = {
  mode: 'floating' | 'full';
  messages: MessageInfo[];
  isLoading: boolean;
  isThinking: boolean;
  thinkingLabel?: string | null;
  onSendMessage: (m: string) => void;
  onEdit: (id: string, text: string) => void;
  onRegenerate: () => void;
};

function MessagesHistory({ mode, messages, isLoading, isThinking, thinkingLabel, onSendMessage, onEdit, onRegenerate }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    ref.current?.scrollTo({ top: ref.current.scrollHeight });
  }, [messages, isThinking]);

  const lastUserId = [...messages].reverse().find((m) => m.messageType === 'user')?.id;
  const visible = messages.filter((m) => {
    // в full welcome не показываем никогда — там свой welcome-screen
    if (mode === 'full' && m.id === 'welcome') return false;
    if (m.messageType === 'thinking' && !m.text.trim()) return false;
    if (m.messageType === 'assistant' && m.id !== 'welcome' && !m.text.trim() && !(m.attachments?.length)) return false;
    return true;
  });

  return (
    <div className="messages-history" ref={ref}>
      {visible.map((m) => (
        <Message
          key={m.id}
          text={m.text}
          messageType={m.messageType}
          createdAt={m.createdAt}
          attachments={m.attachments}
          isLastUser={m.id === lastUserId}
          showSender={mode === 'full'}
          onEdit={(t) => onEdit(m.id, t)}
          onRegenerate={onRegenerate}
        />
      ))}
      {isThinking && <ThinkingIndicator label={thinkingLabel} />}
      {!isThinking && !isLoading && messages.length <= 1 && mode === 'floating' && (
        <Suggestions variant="floating" onSelect={onSendMessage} />
      )}
    </div>
  );
}

export default MessagesHistory;
