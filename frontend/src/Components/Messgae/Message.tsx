import { memo, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeSanitize from 'rehype-sanitize';
import './Message.css';
import type { ChatAttachment } from '../../api/chatApi';
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
    attachments?: ChatAttachment[];
    isLastUser?: boolean;
    showSender?: boolean;
    onEdit?: (t: string) => void;
    onRegenerate?: () => void;
}

function fmt(ts: number) {
    return new Date(ts).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
}

// Все ссылки из markdown открываем в новой вкладке — виджет живёт в iframe.
function LinkNewTab(props: React.AnchorHTMLAttributes<HTMLAnchorElement>) {
    const { href, children, ...rest } = props;
    return (
        <a href={href} target="_blank" rel="noopener noreferrer" {...rest}>
            {children}
        </a>
    );
}

function ComplexImages({ attachments }: { attachments: ChatAttachment[] }) {
    const [failed, setFailed] = useState<Record<string, boolean>>({});
    const imgs = attachments.filter((a) => a && a.type === 'complex_image' && typeof a.url === 'string' && a.url.startsWith('http'));
    if (!imgs.length) return null;
    return (
        <div className="complex-images">
            {imgs.map((a) => (
                failed[a.url] ? null : (
                    <figure className="complex-image" key={a.url}>
                        <img
                            src={a.url}
                            alt={a.title || 'Жилой комплекс'}
                            loading="lazy"
                            onError={() => setFailed((prev) => ({ ...prev, [a.url]: true }))}
                        />
                        {a.title ? (
                            <figcaption>
                                <span className="complex-name">{a.title}</span>
                                <span className="complex-sub">Жилой комплекс</span>
                            </figcaption>
                        ) : null}
                    </figure>
                )
            ))}
        </div>
    );
}

function CopyButton({ text, copiedLabel }: { text: string; copiedLabel: string }) {
    const [copied, setCopied] = useState(false);
    async function copy() {
        try {
            await navigator.clipboard.writeText(text);
            setCopied(true);
            setTimeout(() => setCopied(false), 1200);
        } catch { /* ignore */ }
    }
    return (
        <button
            type="button"
            className="icon-action-btn"
            title={copied ? copiedLabel : 'Копировать'}
            aria-label={copied ? copiedLabel : 'Копировать сообщение'}
            onClick={copy}
        >
            <span className="icon-outline"><CopyOutlineIcon /></span>
            <span className="icon-filled"><CopyIcon /></span>
        </button>
    );
}

export const Message = memo(function Message({ text, messageType, createdAt, attachments, isLastUser, showSender = false, onEdit, onRegenerate }: Props) {
    const [editing, setEditing] = useState(false);
    const [draft, setDraft] = useState(text);
    const [openThink, setOpenThink] = useState(false);

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
                            <textarea value={draft} onChange={(e) => setDraft(e.target.value)} rows={2} aria-label="Редактировать сообщение" />
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
                        <CopyButton text={text} copiedLabel="Скопировано" />
                        {isLastUser && (
                            <>
                                <button type="button" className="icon-action-btn" title="Изменить" aria-label="Изменить сообщение" onClick={() => { setDraft(text); setEditing(true); }}>
                                    <span className="icon-outline"><PenOutlineIcon /></span>
                                    <span className="icon-filled"><PenIcon /></span>
                                </button>
                                <button type="button" className="icon-action-btn" title="Отправить еще раз" aria-label="Отправить еще раз" onClick={() => onRegenerate?.()}>
                                    <span className="icon-single"><RedoIcon /></span>
                                </button>
                            </>
                        )}
                    </div>
                )}
            </div>
        );
    }

    if (messageType === 'sys-info') {
        return (
            <div className="msg-container sys-container">
                <div className="msg sys-info" role="status">
                    <span className="sys-dot" aria-hidden="true" />
                    <span>{text}</span>
                </div>
                <div className="meta"><span>{fmt(createdAt)}</span></div>
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
                <div className="msg error" role="alert">{text}</div>
                <div className="msg-toolbar">
                    <button type="button" className="retry-btn" onClick={() => onRegenerate?.()} aria-label="Повторить запрос">
                        <span className="icon-single"><RedoIcon /></span>
                        <span>Повторить</span>
                    </button>
                </div>
            </div>
        );
    }

    const hasAttachments = !!attachments?.length;

    if (!text.trim() && !hasAttachments) {
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
                {text.trim() ? (
                    <div className="md">
                        <ReactMarkdown
                            remarkPlugins={[remarkGfm]}
                            rehypePlugins={[rehypeSanitize]}
                            components={{ a: LinkNewTab }}
                        >
                            {text}
                        </ReactMarkdown>
                    </div>
                ) : null}
                {hasAttachments ? <ComplexImages attachments={attachments!} /> : null}
                <div className="meta"><span>{fmt(createdAt)}</span></div>
            </div>
            <div className="msg-toolbar">
                <CopyButton text={text} copiedLabel="Скопировано" />
            </div>
        </div>
    );
});

export default Message;
