import MessagesHistory from '../MessagesHistory/MessagesHistory';
import PromptInputField from '../PromptInputField/PromptInputField';
import Suggestions from '../Suggestions/Suggestions';
import './ChatWindow.css';

interface MessageInfo {
  id: string;
  text: string;
  messageType: 'user' | 'assistant' | 'sys-info' | 'error' | 'thinking';
  createdAt: number;
}

interface ChatWindowProps {
  mode: 'floating' | 'full';
  onSendMessage: (message: string) => void;
  onEdit: (id: string, text: string) => void;
  onRegenerate: () => void;
  messages: MessageInfo[];
  isLoading: boolean;
  isThinking: boolean;
}

function ChatWindow({ mode, onSendMessage, onEdit, onRegenerate, messages, isLoading, isThinking }: ChatWindowProps) {
  // новый чат = только welcome, без юзера
  const isNew = messages.length <= 1;

  if (mode === 'full' && isNew && !isLoading && !isThinking) {
    return (
      <div className="chat-window">
        <div className="welcome-screen">
          <h1>Здравствуйте!</h1>
          <p>Я ваш персональный консультант по недвижимости.<br />Задайте любой вопрос — я помогу подобрать квартиру,<br />расскажу о проектах и условиях покупки.</p>
          <Suggestions variant="full" onSelect={onSendMessage} />
        </div>
        <PromptInputField variant="full" disabled={isLoading} onSendMessage={onSendMessage} />
      </div>
    );
  }

  return (
    <div className="chat-window">
      <MessagesHistory mode={mode} messages={messages} isLoading={isLoading} isThinking={isThinking} onSendMessage={onSendMessage} onEdit={onEdit} onRegenerate={onRegenerate} />
      <PromptInputField variant={mode} disabled={isLoading} onSendMessage={onSendMessage} />
    </div>
  );
}

export default ChatWindow;