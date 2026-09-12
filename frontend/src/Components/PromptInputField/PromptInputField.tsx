import { useState, useRef, useEffect } from 'react';
import './PromptInputField.css';
import SendIcon from '../../assets/icons/send.svg?react';
// import AttachIcon from '../../assets/icons/attach-file.svg?react';

interface InputFieldProps {
    onSendMessage: (message: string) => void;
    variant?: 'floating' | 'full';
    disabled?: boolean;
}

function PromptInputField({ onSendMessage, variant = 'floating', disabled = false }: InputFieldProps) {
    const [inputText, setInputText] = useState('');
    const taRef = useRef<HTMLTextAreaElement>(null);

    function autoGrow() {
        const el = taRef.current;
        if (!el) return;
        if (!el.value) {
            el.style.height = 'auto';
            el.style.overflowY = 'hidden';
            return;
        }
        el.style.height = 'auto';
        const max = variant === 'full' ? 132 : 60;
        el.style.height = Math.min(el.scrollHeight, max) + 'px';
        el.style.overflowY = el.scrollHeight > max ? 'auto' : 'hidden';
    }

    useEffect(() => {
        autoGrow();
    }, [inputText, variant]);

    // Фокус в инпут при открытии чата и после ответа (доступность)
    const wasDisabled = useRef(disabled);
    useEffect(() => {
        if (wasDisabled.current && !disabled) {
            taRef.current?.focus();
        }
        wasDisabled.current = disabled;
    }, [disabled]);

    useEffect(() => {
        taRef.current?.focus();
    }, [variant]);

    function send() {
        if (disabled) return;
        if (!inputText.trim()) return;
        onSendMessage(inputText);
        setInputText('');
        requestAnimationFrame(() => {
            if (taRef.current) {
                taRef.current.style.height = 'auto';
                taRef.current.style.overflowY = 'hidden';
            }
        });
    }

    return (
        <div className={'promt-input-field ' + variant}>
            <div className="text-input-holder">
                {/* прикрепление файлов скрыто, вернем когда будет бек
                <button className="icon-btn" title="Прикрепить" type="button" disabled={disabled} onClick={() => alert('Прикрепление файлов скоро')}>
                    <AttachIcon />
                </button>
                */}
                <textarea
                    ref={taRef}
                    id="chat-input"
                    rows={1}
                    placeholder={disabled ? "Дождитесь ответа…" : "Введите ваш вопрос..."}
                    value={inputText}
                    disabled={disabled}
                    onChange={(e) => {
                        setInputText(e.target.value);
                    }}
                    onKeyDown={(e) => {
                        if (e.key === 'Enter' && !e.shiftKey) {
                            e.preventDefault();
                            send();
                        }
                    }}
                    onFocus={(e) => {
                        setTimeout(() => {
                            e.target.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
                        }, 150);
                    }}
                ></textarea>
                <button id="send-prompt-btn" type="button" onClick={send} aria-label="Отправить" disabled={disabled}>
                    <SendIcon />
                </button>
            </div>
            <div className="promt-footer-note">
                Искусственный интеллект может допускать ошибки. Проверяйте важную информацию у менеджера.
            </div>
        </div>
    );
}

export default PromptInputField;