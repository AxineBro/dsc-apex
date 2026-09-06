export type SuggestionIcon =
  | 'house'
  | 'bed'
  | 'park'
  | 'sunny'
  | 'search'
  | 'work'
  | 'docs'
  | 'folder'
  | 'construction'
  | 'chat';

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
    id: 'pick',
    title: 'Подбор',
    items: [
      { id: 'pick-1', label: 'Подобрать квартиру по параметрам', prompt: 'Помоги подобрать квартиру: 2-комнатная, до 6 млн, средний этаж', icon: 'house' },
      { id: 'pick-2', label: 'Двушка в Европейском', prompt: 'Какие есть двухкомнатные квартиры в ЖК Европейский?', icon: 'bed' },
      { id: 'pick-3', label: 'С видом на парк', prompt: 'Покажи квартиры с видом на парк', icon: 'park' },
      { id: 'pick-4', label: 'На солнечной стороне', prompt: 'Какие квартиры на солнечной стороне?', icon: 'sunny' },
      { id: 'pick-5', label: 'До 6 млн рублей', prompt: 'Что есть до 6 млн рублей?', icon: 'search' },
    ],
  },
  {
    id: 'mortgage',
    title: 'Ипотека',
    items: [
      { id: 'mor-1', label: 'Ипотечные программы', prompt: 'Какие ипотечные программы есть?', icon: 'work' },
      { id: 'mor-2', label: 'Первый взнос и платеж', prompt: 'Какой первый взнос и какой будет ежемесячный платеж?', icon: 'docs' },
      { id: 'mor-3', label: 'Рассрочка от застройщика', prompt: 'Есть ли рассрочка от застройщика?', icon: 'folder' },
      { id: 'mor-4', label: 'Маткапитал', prompt: 'Можно ли купить с маткапиталом?', icon: 'house' },
      { id: 'mor-5', label: 'Ставка и условия', prompt: 'Какая сейчас ставка и условия ипотеки?', icon: 'search' },
    ],
  },
  {
    id: 'build',
    title: 'Стройка',
    items: [
      { id: 'bld-1', label: 'Ход строительства', prompt: 'Какой ход строительства сейчас?', icon: 'construction' },
      { id: 'bld-2', label: 'Когда сдача?', prompt: 'Когда сдача ЖК Крымский квартал?', icon: 'house' },
      { id: 'bld-3', label: 'Какие ЖК уже сданы?', prompt: 'Какие ЖК уже сданы?', icon: 'folder' },
      { id: 'bld-4', label: 'Инфраструктура рядом', prompt: 'Что с инфраструктурой рядом: парки, школы?', icon: 'park' },
      { id: 'bld-5', label: 'Кто застройщик?', prompt: 'Кто застройщик и сколько домов сдано?', icon: 'work' },
    ],
  },
  {
    id: 'docs',
    title: 'Документы',
    items: [
      { id: 'doc-1', label: 'Документы для покупки', prompt: 'Какие документы нужны для покупки квартиры?', icon: 'docs' },
      { id: 'doc-2', label: 'Сделка дистанционно', prompt: 'Можно ли оформить сделку дистанционно?', icon: 'folder' },
      { id: 'doc-3', label: 'Регистрация сделки', prompt: 'Сколько длится регистрация сделки?', icon: 'work' },
      { id: 'doc-4', label: 'Покупка по доверенности', prompt: 'Можно ли купить по доверенности?', icon: 'house' },
      { id: 'doc-5', label: 'Связаться с менеджером', prompt: 'Хочу связаться с менеджером', icon: 'chat' },
    ],
  },
];

// по 1 случайной из каждой группы = всегда 4 штуки из разных групп
export function getRandomStarter(): SuggestionItem[] {
  return SUGGESTION_GROUPS.map((g) => {
    const i = Math.floor(Math.random() * g.items.length);
    return g.items[i];
  });
}