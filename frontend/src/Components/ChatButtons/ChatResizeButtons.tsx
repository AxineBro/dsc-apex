import './ChatResizeButtons.css'
import Fullscreen from '../../assets/icons/fullscreen.svg?react'
import CloseFullscreen from '../../assets/icons/close-fullscreen.svg?react'
import Close from '../../assets/icons/close.svg?react'

type ChatModes = 'closed' | 'floating' | 'full';

interface ChatProps {
    mode: ChatModes;
    onModeChange: (mode: ChatModes) => void;
}

function ChatResizeButtons({ mode, onModeChange }: ChatProps) {
    return (
        <div className='resize-close-buttons'>
            <button
                onClick={() => onModeChange(mode === 'floating' ? 'full' : 'floating')}
                id='resize-chat-button'
                className={'chat-button ' + mode}>
                {(mode === 'floating') && (
                    <Fullscreen id='resize-chat-button-img'/>
                )}
                {(mode === 'full') && (
                    <CloseFullscreen id='resize-chat-button-img'/>
                )}
            </button>
            <button
                onClick={() => onModeChange('closed')}
                className={'chat-button ' + mode}
                id='close-chat-button'>
                <Close id='close-chat-button-img'/>
            </button>
        </div>
    )
}

export default ChatResizeButtons;