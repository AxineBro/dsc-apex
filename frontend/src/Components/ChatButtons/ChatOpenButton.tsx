import './ChatOpenButton.css'
import ChatIcon from '../../assets/icons/chat.svg?react';

type ChatModes = 'closed' | 'floating' | 'full';

interface ChatProps {
    onModeChange: (mode: ChatModes) => void;
}

function ChatOpenButton({ onModeChange }: ChatProps) {
    function open() {
        const isMobile = window.matchMedia('(max-width: 640px)').matches;
        onModeChange(isMobile ? 'full' : 'floating');
    }

    return (
        <>
            <button
                className='chat-open-button'
                onClick={open}>
                <ChatIcon id='chat-icon'/>
            </button>
        </>
    )
}
export default ChatOpenButton;