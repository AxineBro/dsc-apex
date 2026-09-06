import SideMenu from '../SideMenu/SideMenu';
import ChatWindow from '../ChatWindow/ChatWindow';
import ChatResizeButtons from '../ChatButtons/ChatResizeButtons';
import './ChatLayout.css';
import DskLogo from '../../assets/icons/dsk_logo_32x32.png';

type ChatModes = 'closed' | 'floating' | 'full';

interface MessageInfo {
  id: string;
  text: string;
  messageType: 'user' | 'assistant' | 'sys-info' | 'error' | 'thinking';
  createdAt: number;
}

interface ChatProps {
  mode: ChatModes;
  onModeChange: (mode: ChatModes) => void;
  onSendMessage: (message: string) => void;
  onEdit: (id: string, text: string) => void;
  onRegenerate: () => void;
  onNewChat?: () => void;
  messages: MessageInfo[];
  isLoading: boolean;
  isThinking: boolean;
}

function ChatLayout({ mode, onModeChange, onSendMessage, onEdit, onRegenerate, onNewChat, messages, isLoading, isThinking }: ChatProps) {
  if (mode === 'closed') return null;

  if (mode === 'full') {
    return (
      <div className="chat-layout full">
        <SideMenu isVisible={true} onNewChat={onNewChat} />
        <div className="chat-main">
          <div className="floating-controls">
            <ChatResizeButtons onModeChange={onModeChange} mode={mode} />
          </div>
          <ChatWindow mode={mode} messages={messages} isLoading={isLoading} isThinking={isThinking} onSendMessage={onSendMessage} onEdit={onEdit} onRegenerate={onRegenerate} />
        </div>
      </div>
    );
  }

  return (
    <div className="chat-layout floating">
      <div className="chat-header">
        <div className="chat-header-left">
          <img src={DskLogo} alt="DSK" className="chat-logo" />
          <div>
            <div className="chat-title">Консультант ДСК</div>
            <div className="chat-status"><span className="dot" />Онлайн</div>
          </div>
        </div>
        <ChatResizeButtons onModeChange={onModeChange} mode={mode} />
      </div>
      <div className="chat-body">
        <ChatWindow mode={mode} messages={messages} isLoading={isLoading} isThinking={isThinking} onSendMessage={onSendMessage} onEdit={onEdit} onRegenerate={onRegenerate} />
      </div>
    </div>
  );
}

export default ChatLayout;