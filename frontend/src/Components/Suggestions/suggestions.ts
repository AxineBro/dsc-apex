export type SuggestionIcon =
  | 'house'
  | 'bed'
  | 'park'
  | 'sunny'
  | 'search'
  | 'docs'
  | 'folder'
  | 'construction'
  | 'chat'
  | 'money'
  | 'man'
  | 'education'
  | 'parking'
  | 'garage'
  | 'city'
  | 'color';

export interface SuggestionItem {
  id: string;
  label: string;
  prompt: string;
  icon: SuggestionIcon;
}

export interface SuggestionGroup {
  id: string;
  title: string;
  items: SuggestionItem[];
}

export const SUGGESTION_GROUPS: SuggestionGroup[] = [
  {
    id: 'params',
    title: 'Подбор',
    items: [
      { id: 'par-1', label: '2-комнатная 50–60 м²', prompt: 'Подбери 2-комнатную квартиру площадью 50–60 м²', icon: 'bed' },
      { id: 'par-2', label: '1-комнатная с отделкой', prompt: 'Покажи 1-комнатные квартиры с отделкой', icon: 'color' },
      { id: 'par-3', label: '3-комнатная для семьи', prompt: 'Нужна 3-комнатная квартира для семьи с детьми', icon: 'man' },
      { id: 'par-4', label: 'Не первый этаж', prompt: 'Подбери квартиру не на первом и не на последнем этаже', icon: 'sunny' },
      { id: 'par-5', label: 'С лоджией', prompt: 'Покажи квартиры с лоджией или балконом', icon: 'search' },
    ],
  },
  {
    id: 'place',
    title: 'Район',
    items: [
      { id: 'pla-1', label: 'У парка', prompt: 'Покажи квартиры рядом с парком', icon: 'park' },
      { id: 'pla-2', label: 'В тихом районе', prompt: 'Какие квартиры есть в тихом спальном районе?', icon: 'city' },
      { id: 'pla-3', label: 'Рядом со школой', prompt: 'Нужна квартира рядом со школой и детским садом', icon: 'education' },
      { id: 'pla-4', label: 'С видом на город', prompt: 'Покажи квартиры с видом на город', icon: 'city' },
      { id: 'pla-5', label: 'Рядом с транспортом', prompt: 'Какие квартиры рядом с остановкой и транспортом?', icon: 'search' },
    ],
  },
  {
    id: 'price',
    title: 'Бюджет',
    items: [
      { id: 'pri-1', label: 'До 6 млн рублей', prompt: 'Что есть до 6 миллионов рублей?', icon: 'money' },
      { id: 'pri-2', label: 'Двушка до 7 млн', prompt: 'Найди 2-комнатную квартиру до 7 миллионов', icon: 'money' },
      { id: 'pri-3', label: 'Сравнить по цене', prompt: 'Сравни варианты по цене и площади', icon: 'money' },
      { id: 'pri-4', label: 'Самая выгодная', prompt: 'Какой вариант самый выгодный по цене за метр?', icon: 'money' },
      { id: 'pri-5', label: 'До 5 млн', prompt: 'Покажи квартиры до 5 миллионов рублей', icon: 'money' },
    ],
  },
  {
    id: 'features',
    title: 'Характеристики',
    items: [
      { id: 'fea-1', label: 'С парковкой', prompt: 'Есть ли квартиры с парковкой или паркингом?', icon: 'parking' },
      { id: 'fea-2', label: 'С гаражом', prompt: 'Есть ли варианты с гаражом?', icon: 'garage' },
      { id: 'fea-3', label: 'С кладовой', prompt: 'Покажи квартиры с кладовой', icon: 'folder' },
      { id: 'fea-4', label: 'С отделкой', prompt: 'Какие квартиры сдаются с отделкой?', icon: 'color' },
      { id: 'fea-5', label: 'Средний этаж', prompt: 'Подбери квартиру на среднем этаже', icon: 'construction' },
    ],
  },
];

export function getRandomStarter(): SuggestionItem[] {
  return SUGGESTION_GROUPS.map((g) => {
    const i = Math.floor(Math.random() * g.items.length);
    return g.items[i];
  });
}