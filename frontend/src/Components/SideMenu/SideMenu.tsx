import { useState } from 'react';
import './SideMenu.css';
import AddIcon from '../../assets/icons/add.svg?react';
import SearchIcon from '../../assets/icons/search.svg?react';
import MenuIcon from '../../assets/icons/menu.svg?react';
import MenuCloseIcon from '../../assets/icons/menu-close.svg?react';
import CrossCloseIcon from '../../assets/icons/cross-close.svg?react';
import BubbleIcon from '../../assets/icons/chat-bubble-outline.svg?react';
import BubbleFillIcon from '../../assets/icons/chat-bubble.svg?react';
import DeleteIcon from '../../assets/icons/delete.svg?react';
import DeleteOutlineIcon from '../../assets/icons/delete-outline.svg?react';

interface ChatItem {
  id: string;
  title: string;
  updatedAt: number;
}

interface SideMenuProps {
  isVisible: boolean;
  chats: ChatItem[];
  activeChatId: string | null;
  onSelectChat: (id: string) => void;
  onDeleteChat: (id: string) => void;
  onNewChat?: () => void;
}

function fmtTime(ts: number) {
  const d = new Date(ts);
  const now = new Date();
  const sameDay = d.toDateString() === now.toDateString();
  if (sameDay) return d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
  const diff = Math.floor((now.getTime() - d.getTime()) / 86400000);
  if (diff === 1) return 'Вчера';
  if (diff < 5) return `${diff} дн. назад`;
  return d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });
}

function SideMenu({ isVisible, chats, activeChatId, onSelectChat, onDeleteChat, onNewChat }: SideMenuProps) {
  const [collapsed, setCollapsed] = useState<boolean>(() => {
    try {
      return window.matchMedia('(max-width: 640px)').matches;
    } catch {
      return false;
    }
  });
  const [query, setQuery] = useState('');
  const [showAll, setShowAll] = useState(false);

  if (!isVisible) return null;

  const filtered = chats.filter((c) =>
    c.title.toLowerCase().includes(query.toLowerCase().trim())
  );
  const visible = showAll ? filtered : filtered.slice(0, 7);

  return (
    <div className={'side-menu' + (collapsed ? ' collapsed' : '')}>
      <div className="side-top">
        <button
          type="button"
          className="menu-toggle"
          title={collapsed ? 'Показать меню' : 'Скрыть меню'}
          onClick={() => setCollapsed(!collapsed)}
        >
          {collapsed ? (
            <MenuIcon />
          ) : (
            <>
              <span className="icon-desktop"><MenuCloseIcon /></span>
              <span className="icon-mobile"><CrossCloseIcon /></span>
            </>
          )}
        </button>
      </div>

      <div className="side-content" aria-hidden={collapsed}>
        <button
          type="button"
          className="new-chat-btn"
          onClick={() => { onNewChat?.(); }}
          tabIndex={collapsed ? -1 : 0}
        >
          <AddIcon />
          <span>Новый чат</span>
        </button>

        <div className="side-search">
          <SearchIcon />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Поиск по чатам"
            tabIndex={collapsed ? -1 : 0}
          />
        </div>

        <div className="side-label">Недавние чаты</div>

        <div className="side-list">
          {visible.map((c) => {
            const active = c.id === activeChatId;
            return (
              <div key={c.id} className={'side-item' + (active ? ' active' : '')}>
                <button
                  type="button"
                  className="side-item-main"
                  onClick={() => onSelectChat(c.id)}
                  tabIndex={collapsed ? -1 : 0}
                >
                  <span className="side-item-icon">{active ? <BubbleFillIcon /> : <BubbleIcon />}</span>
                  <span className="side-item-title">{c.title}</span>
                  <span className="side-item-time">{fmtTime(c.updatedAt)}</span>
                </button>
                <button
                  type="button"
                  className="side-item-delete"
                  title="Удалить чат"
                  onClick={(e) => { e.stopPropagation(); onDeleteChat(c.id); }}
                  tabIndex={collapsed ? -1 : 0}
                >
                  <span className="icon-outline"><DeleteOutlineIcon /></span>
                  <span className="icon-filled"><DeleteIcon /></span>
                </button>
              </div>
            );
          })}
          {chats.length === 0 && <div className="side-empty">Пока нет чатов — напишите первое сообщение</div>}
          {chats.length > 0 && visible.length === 0 && <div className="side-empty">Ничего не найдено</div>}
        </div>

        {!showAll && filtered.length > 7 && (
          <button
            type="button"
            className="show-more-btn"
            onClick={() => setShowAll(true)}
            tabIndex={collapsed ? -1 : 0}
          >
            Показать ещё
          </button>
        )}
      </div>
    </div>
  );
}

export default SideMenu;