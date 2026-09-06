import { memo, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeSanitize from 'rehype-sanitize';
import './Message.css';
import CopyIcon from '../../assets/icons/copy.svg?react';
import CopyOutlineIcon from '../../assets/icons/copy-outline.svg?react';
import PenIcon from '../../assets/icons/pen.svg?react';
import PenOutlineIcon from '../../assets/icons/pen-outline.svg?react';
import RedoIcon from '../../assets/icons/redo.svg?react';
import DskLogo from '../../assets/icons/dsk_logo_32x32.png';

interface Props {
    text: string;
    messageType: 'user' | 'assistant' | 'sys-info' | 'error' | 'thinking';
    createdAt: number;
    isLastUser?: boolean;
    showSender?: boolean;
    onEdit?: (t: string) => void;
    onRegenerate?: () => void;
}

function fmt(ts: number) {
    return new Date(ts).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
}

export const Message = memo(function Message({ text, messageType, createdAt, isLastUser, showSender = false, onEdit, onRegenerate }: Props) {
    const [copied, setCopied] = useState(false);
    const [editing, setEditing] = useState(false);
    const [draft, setDraft] = useState(text);
    const [openThink, setOpenThink] = useState(false);

    async function copy() {
        try {
            await navigator.clipboard.writeText(text);
            setCopied(true);
            setTimeout(() => setCopied(false), 1200);
        } catch { /* ignore */ }
    }

    if (messageType === 'thinking') {
        if (!text.trim()) return null;
        return (
            <div className="msg-container assistant-container">
                {showSender && (
                    <div className="ai-sender">
                        <img src={DskLogo} alt="DSK" />
                        <span>Консультант ДСК</span>
                    </div>
                )}
                <div className="msg thinking">
                    <div className="thinking-header">
                        <p>Думаю...</p>
                        <button onClick={() => setOpenThink(!openThink)}>
                            {openThink ? 'Скрыть' : 'Развернуть'}
                        </button>
                    </div>
                    {openThink && (
                        <div className="md">
                            <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeSanitize]}>
                                {text}
                            </ReactMarkdown>
                        </div>
                    )}
                </div>
            </div>
        );
    }

    if (messageType === 'user') {
        return (
            <div className="msg-container user-container">
                <div className="msg user">
                    {editing ? (
                        <div className="edit-box">
                            <textarea value={draft} onChange={(e) => setDraft(e.target.value)} rows={2} />
                            <div className="edit-actions">
                                <button type="button" onClick={() => setEditing(false)}>Отмена</button>
                                <button type="button" className="primary" onClick={() => { onEdit?.(draft); setEditing(false); }}>Отправить</button>
                            </div>
                        </div>
                    ) : (
                        <>
                            <div className="md">{text}</div>
                            <div className="meta"><span>{fmt(createdAt)}</span></div>
                        </>
                    )}
                </div>
                {!editing && (
                    <div className="msg-toolbar">
                        <button type="button" className="icon-action-btn" title={copied ? 'Скопировано' : 'Копировать'} onClick={copy}>
                            <span className="icon-outline"><CopyOutlineIcon /></span>
                            <span className="icon-filled"><CopyIcon /></span>
                        </button>
                        {isLastUser && (
                            <>
                                <button type="button" className="icon-action-btn" title="Изменить" onClick={() => { setDraft(text); setEditing(true); }}>
                                    <span className="icon-outline"><PenOutlineIcon /></span>
                                    <span className="icon-filled"><PenIcon /></span>
                                </button>
                                <button type="button" className="icon-action-btn" title="Отправить еще раз" onClick={() => onRegenerate?.()}>
                                    <span className="icon-single"><RedoIcon /></span>
                                </button>
                            </>
                        )}
                    </div>
                )}
            </div>
        );
    }

    if (messageType === 'error') {
        return (
            <div className="msg-container assistant-container">
                {showSender && (
                    <div className="ai-sender">
                        <img src={DskLogo} alt="DSK" />
                        <span>Консультант ДСК</span>
                    </div>
                )}
                <div className="msg error">{text}</div>
            </div>
        );
    }

    if (!text.trim()) {
        return (
            <div className="msg-container assistant-container">
                {showSender && (
                    <div className="ai-sender">
                        <img src={DskLogo} alt="DSK" />
                        <span>Консультант ДСК</span>
                    </div>
                )}
                <div className="msg assistant">печатает...</div>
            </div>
        );
    }

    return (
        <div className="msg-container assistant-container">
            {showSender && (
                <div className="ai-sender">
                    <img src={DskLogo} alt="DSK" />
                    <span>Консультант ДСК</span>
                </div>
            )}
            <div className="msg assistant">
                <div className="md">
                    <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeSanitize]}>
                        {text}
                    </ReactMarkdown>
                </div>
                <div className="meta"><span>{fmt(createdAt)}</span></div>
            </div>
            <div className="msg-toolbar">
                <button type="button" className="icon-action-btn" title={copied ? 'Скопировано' : 'Копировать'} onClick={copy}>
                    <span className="icon-outline"><CopyOutlineIcon /></span>
                    <span className="icon-filled"><CopyIcon /></span>
                </button>
            </div>
        </div>
    );
});

export default Message;