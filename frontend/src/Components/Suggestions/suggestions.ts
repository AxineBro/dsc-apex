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

/**
 * Только честные подсказки: бэк фильтрует комнаты/площадь/бюджет/этаж/ЖК/паркинг.
 * Про отделку, лоджию, кладовку, гараж, школу и метро чипс не обещает —
 * таких полей в Filters нет, иначе модель игнорирует или выдумывает.
 */
export const SUGGESTION_GROUPS: SuggestionGroup[] = [
  {
    id: 'params',
    title: 'Подбор',
    items: [
      { id: 'par-1', label: '2-комнатная 50–60 м²', prompt: 'Подбери 2-комнатную квартиру площадью 50–60 м²', icon: 'bed' },
      { id: 'par-2', label: '1-комнатная до 6 млн', prompt: 'Покажи 1-комнатные квартиры до 6 миллионов рублей', icon: 'bed' },
      { id: 'par-3', label: '3-комнатная для семьи', prompt: 'Нужна 3-комнатная квартира для семьи с детьми', icon: 'man' },
      { id: 'par-4', label: 'Не первый этаж', prompt: 'Подбери квартиру не на первом этаже', icon: 'sunny' },
      { id: 'par-5', label: 'Средний этаж', prompt: 'Подбери квартиру на среднем этаже', icon: 'construction' },
    ],
  },
  {
    id: 'place',
    title: 'Жилой комплекс',
    items: [
      { id: 'pla-1', label: 'Ласточкино', prompt: 'Покажи квартиры в ЖК Ласточкино', icon: 'house' },
      { id: 'pla-2', label: 'Яблоневые сады', prompt: 'Покажи квартиры в ЖК Яблоневые сады', icon: 'house' },
      { id: 'pla-3', label: 'Ленинградский квартал', prompt: 'Покажи квартиры в ЖК Ленинградский квартал', icon: 'city' },
      { id: 'pla-4', label: 'Европейский', prompt: 'Покажи квартиры в ЖК Европейский', icon: 'city' },
      { id: 'pla-5', label: 'Крымский квартал', prompt: 'Покажи квартиры в ЖК Крымский квартал', icon: 'park' },
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
      { id: 'fea-2', label: '2-комнатная с парковкой', prompt: 'Найди 2-комнатную квартиру с парковкой', icon: 'parking' },
      { id: 'fea-3', label: 'На высоком этаже', prompt: 'Покажи квартиры на высоком этаже', icon: 'construction' },
      { id: 'fea-4', label: 'Просторная двушка', prompt: 'Найди 2-комнатную квартиру площадью от 55 м²', icon: 'search' },
      { id: 'fea-5', label: 'О квартире и доме', prompt: 'Расскажи про жилой комплекс и срок сдачи дома', icon: 'docs' },
    ],
  },
];

export function getRandomStarter(): SuggestionItem[] {
  return SUGGESTION_GROUPS.map((g) => {
    const i = Math.floor(Math.random() * g.items.length);
    return g.items[i];
  });
}
