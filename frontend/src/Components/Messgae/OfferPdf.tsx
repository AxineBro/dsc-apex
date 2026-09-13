import PdfIcon from '../../assets/icons/pdf.svg?react';
import { resolveAttachmentUrl, type ChatAttachment } from '../../api/chatApi';

function fileNameFromUrl(url: string, fallback = 'KP-DSK.pdf'): string {
  const m = url.match(/\/offers\/([^/]+)\/pdf/i);
  if (m) return `KP-DSK-${m[1]}.pdf`;
  return fallback;
}

/**
 * Карточка PDF-файла КП от бэка (attachments: { type: 'offer_pdf', url: '/api/v1/offers/<id>/pdf' }).
 * Одна кнопка — нативная ссылка с download: браузер сам скачивает файл,
 * без fetch/JS, поэтому не ломается о блокировщики всплывашек и CORS.
 */
export default function OfferPdf({ attachment }: { attachment: ChatAttachment }) {
  const raw = (attachment?.url || '').trim();
  if (!raw) return null;
  const url = resolveAttachmentUrl(raw);
  const title = attachment.title?.trim() || 'Коммерческое предложение (PDF)';

  return (
    <div className="offer-pdf" role="article" aria-label={title}>
      <div className="offer-pdf-row">
        <span className="offer-pdf-icon" aria-hidden="true"><PdfIcon /></span>
        <div className="offer-pdf-titles">
          <div className="offer-pdf-title">{title}</div>
          <div className="offer-pdf-sub">PDF-документ · ДСК</div>
        </div>
        <span className="offer-pdf-badge">PDF</span>
      </div>

      <div className="offer-pdf-actions">
        <a
          className="offer-btn"
          href={url}
          download={fileNameFromUrl(raw)}
          title="Скачать PDF-файл на устройство"
        >
          ⬇ Скачать PDF
        </a>
      </div>
    </div>
  );
}
