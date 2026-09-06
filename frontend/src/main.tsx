import { createRoot } from 'react-dom/client';
import iframeCss from './InitWidget.css?inline';
import ChatWidget from './Components/ChatWidget/ChatWidget';

const cssFiles = import.meta.glob(
  './Components/**/*.css',
  { query: '?inline', import: 'default', eager: true },
) as Record<string, string>;

const widgetCss = Object.values(cssFiles).join('\n');

let iframe: HTMLIFrameElement | null = null;

function mount() {
  if (iframe) return;

  iframe = document.createElement('iframe');
  iframe.className = 'dsk-chat-iframe chat-closed';
  iframe.title = 'DSK AI consultant';

  document.body.appendChild(iframe);

  const iframeDocument = iframe.contentDocument;
  if (!iframeDocument) {
    throw new Error('Не удалось получить document iframe');
  }

  const meta = iframeDocument.createElement('meta');
  meta.name = 'viewport';
  meta.content = 'width=device-width, initial-scale=1, interactive-widget=resizes-content';
  iframeDocument.head.appendChild(meta);

  const widgetStyle = iframeDocument.createElement('style');
  widgetStyle.textContent = widgetCss;
  iframeDocument.head.appendChild(widgetStyle);

  const parentStyle = document.createElement('style');
  parentStyle.textContent = iframeCss;
  document.head.appendChild(parentStyle);

  const container = iframeDocument.createElement('div');
  container.id = 'ai-chat-iframe-container';
  iframeDocument.body.appendChild(container);

  createRoot(container).render(<ChatWidget />);
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', mount);
} else {
  mount();
}

window.addEventListener('message', (event) => {
  if (!iframe) return;
  if (event.data?.type !== 'CHAT_MODE_CHANGE') return;

  if (event.data.mode === 'closed') {
    document.body.style.overflow = 'auto';
    iframe.className = 'dsk-chat-iframe chat-closed';
  }
  if (event.data.mode === 'floating') {
    document.body.style.overflow = 'auto';
    iframe.className = 'dsk-chat-iframe chat-floating';
  }
  if (event.data.mode === 'full') {
    document.body.style.overflow = 'hidden';
    iframe.className = 'dsk-chat-iframe chat-full';
  }
});