import { useEffect, useState } from 'react';
import './ThinkingIndicator.css';

import SearchIcon from '../../assets/icons/search.svg?react';
import VariantsIcon from '../../assets/icons/looking_variants.svg?react';
import TableIcon from '../../assets/icons/table-chart-view.svg?react';
import HouseIcon from '../../assets/icons/house.svg?react';
import DocsIcon from '../../assets/icons/docs.svg?react';
import WorkIcon from '../../assets/icons/work.svg?react';
import ConstructionIcon from '../../assets/icons/construction.svg?react';
import FolderIcon from '../../assets/icons/folder.svg?react';
import ChatIcon from '../../assets/icons/chat-bubble-outline.svg?react';

const STEPS = [
  { text: 'Думаю…', Icon: ChatIcon },
  { text: 'Анализирую запрос…', Icon: SearchIcon },
  { text: 'Смотрю варианты…', Icon: VariantsIcon },
  { text: 'Сравниваю квартиры…', Icon: TableIcon },
  { text: 'Проверяю цены и площади…', Icon: DocsIcon },
  { text: 'Уточняю детали…', Icon: WorkIcon },
  { text: 'Проверяю наличие…', Icon: FolderIcon },
  { text: 'Подбираю лучшее…', Icon: HouseIcon },
  { text: 'Формирую ответ…', Icon: ConstructionIcon },
];

function ThinkingIndicator() {
  const [idx, setIdx] = useState(0);

  useEffect(() => {
    let alive = true;
    let t: ReturnType<typeof setTimeout>;
    function tick() {
      t = setTimeout(() => {
        if (!alive) return;
        setIdx((i) => (i + 1) % STEPS.length);
        tick();
      }, 4500 + Math.random() * 1000);
    }
    t = setTimeout(() => {
      if (!alive) return;
      setIdx(1);
      tick();
    }, 4500 + Math.random() * 1000);
    return () => { alive = false; clearTimeout(t); };
  }, []);

  const { text, Icon } = STEPS[idx];

  return (
    <div className="thinking-plain">
      <span className="ti-dots"><span /><span /><span /></span>
      <span className="ti-icon"><Icon /></span>
      <span className="ti-text" key={idx}>{text}</span>
    </div>
  );
}

export default ThinkingIndicator;