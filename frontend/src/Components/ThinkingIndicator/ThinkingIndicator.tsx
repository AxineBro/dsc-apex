import { useEffect, useState } from 'react';
import './ThinkingIndicator.css';

import SearchIcon from '../../assets/icons/search.svg?react';
import VariantsIcon from '../../assets/icons/looking_variants.svg?react';
import TableIcon from '../../assets/icons/table-chart-view.svg?react';
import HouseIcon from '../../assets/icons/house.svg?react';
import DocsIcon from '../../assets/icons/docs.svg?react';
import ParkIcon from '../../assets/icons/park.svg?react';
import MoneyIcon from '../../assets/icons/money.svg?react';
import ManIcon from '../../assets/icons/man.svg?react';
import ChatIcon from '../../assets/icons/chat-bubble-outline.svg?react';

const STEPS = [
  { text: 'Думаю…', Icon: ChatIcon },
  { text: 'Анализирую запрос…', Icon: SearchIcon },
  { text: 'Смотрю варианты…', Icon: VariantsIcon },
  { text: 'Сравниваю планировки…', Icon: TableIcon },
  { text: 'Проверяю площади…', Icon: DocsIcon },
  { text: 'Ищу у парка…', Icon: ParkIcon },
  { text: 'Считаю бюджет…', Icon: MoneyIcon },
  { text: 'Подбираю для семьи…', Icon: ManIcon },
  { text: 'Формирую ответ…', Icon: HouseIcon },
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