import { useMemo } from 'react';
import { getRandomStarter, type SuggestionIcon } from './suggestions';
import './Suggestions.css';

import HouseIcon from '../../assets/icons/house.svg?react';
import BedIcon from '../../assets/icons/bed.svg?react';
import ParkIcon from '../../assets/icons/park.svg?react';
import SunnyIcon from '../../assets/icons/sunny.svg?react';
import SearchIcon from '../../assets/icons/search.svg?react';
import WorkIcon from '../../assets/icons/work.svg?react';
import DocsIcon from '../../assets/icons/docs.svg?react';
import FolderIcon from '../../assets/icons/folder.svg?react';
import ConstructionIcon from '../../assets/icons/construction.svg?react';
import ChatIcon from '../../assets/icons/chat-bubble-outline.svg?react';

const ICONS: Record<SuggestionIcon, React.ComponentType> = {
  house: HouseIcon,
  bed: BedIcon,
  park: ParkIcon,
  sunny: SunnyIcon,
  search: SearchIcon,
  work: WorkIcon,
  docs: DocsIcon,
  folder: FolderIcon,
  construction: ConstructionIcon,
  chat: ChatIcon,
};

interface Props {
  variant: 'floating' | 'full';
  onSelect: (prompt: string) => void;
}

function Suggestions({ variant, onSelect }: Props) {
  const items = useMemo(() => getRandomStarter(), []);

  return (
    <div className={variant === 'full' ? 'sugg-full' : 'sugg-floating'}>
      {items.map((s) => {
        const Icon = ICONS[s.icon];
        return (
          <button key={s.id} type="button" className={variant === 'full' ? 'sugg-card' : 'sugg-chip'} onClick={() => onSelect(s.prompt)}>
            <span className="sugg-icon"><Icon /></span>
            <span className="sugg-label">{s.label}</span>
          </button>
        );
      })}
    </div>
  );
}

export default Suggestions;