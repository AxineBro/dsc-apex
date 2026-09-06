import { useState, useEffect } from 'react';
import './SideMenu.css';
import AddIcon from '../../assets/icons/add.svg?react';
import SearchIcon from '../../assets/icons/search.svg?react';
import MenuIcon from '../../assets/icons/menu.svg?react';
import MenuCloseIcon from '../../assets/icons/menu-close.svg?react';
import CrossCloseIcon from '../../assets/icons/cross-close.svg?react';
import BubbleIcon from '../../assets/icons/chat-bubble-outline.svg?react';
import BubbleFillIcon from '../../assets/icons/chat-bubble.svg?react';

interface SideMenuProps {
  isVisible: boolean;
  onNewChat?: () => void;
}

// заглушка: история будет с сервера, пока кликабельный фейк
const FAKE_CHATS = [
  { id: '1', title: 'Подбор квартиры', time: '10:45' },
  { id: '2', title: 'Ипотечные программы', time: '10:30' },
  { id: '3', title: 'Ход строительства ЖК', time: 'Вчера' },
  { id: '4', title: 'Документы для покупки', time: 'Вчера' },
  { id: '5', title: 'Отделка квартир', time: '2 дн. назад' },
  { id: '6', title: 'Парковка и кладовые', time: '3 дн. назад' },
  { id: '7', title: 'Как купить квартиру', time: '4 дн. назад' },
  { id: '8', title: 'Сдача Крымского квартала', time: '5 дн. назад' },
  { id: '9', title: 'Рассрочка от застройщика', time: 'неделю назад' },
];

function SideMenu({ isVisible, onNewChat }: SideMenuProps) {
  // на мобиле по умолчанию закрыто
  const [collapsed, setCollapsed] = useState<boolean>(() => {
    try {
      return window.matchMedia('(max-width: 640px)').matches;
    } catch {
      return false;
    }
  });
  const [query, setQuery] = useState('');
  const [showAll, setShowAll] = useState(false);
  const [activeId, setActiveId] = useState('1');
  const [noAnim, setNoAnim] = useState(true);

  // отключаем transition на первые кадры, чтобы меню не анимировало ширину при маунте full
  useEffect(() => {
    const t = setTimeout(() => setNoAnim(false), 300);
    return () => clearTimeout(t);
  }, []);

  if (!isVisible) return null;

  const filtered = FAKE_CHATS.filter((c) =>
    c.title.toLowerCase().includes(query.toLowerCase().trim())
  );
  const visible = showAll ? filtered : filtered.slice(0, 7);

  return (
    <div className={'side-menu' + (collapsed ? ' collapsed' : '') + (noAnim ? ' no-anim' : '')}>
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
          onClick={() => { setActiveId(''); onNewChat?.(); }}
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
            const active = c.id === activeId;
            return (
              <button
                key={c.id}
                type="button"
                className={'side-item' + (active ? ' active' : '')}
                onClick={() => setActiveId(c.id)}
                tabIndex={collapsed ? -1 : 0}
              >
                <span className="side-item-icon">{active ? <BubbleFillIcon /> : <BubbleIcon />}</span>
                <span className="side-item-title">{c.title}</span>
                <span className="side-item-time">{c.time}</span>
              </button>
            );
          })}
          {visible.length === 0 && <div className="side-empty">Ничего не найдено</div>}
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