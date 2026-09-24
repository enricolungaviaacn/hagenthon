import { useEffect, useRef, useState } from 'react';
import * as pdfjsLib from 'pdfjs-dist';

pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.min.mjs',
  import.meta.url
).toString();

interface Props {
  url: string;
  token: string;
}

export default function PdfViewer({ url, token }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!url) return;

    let cancelled = false;
    setLoading(true);
    setError('');

    const load = async () => {
      try {
        const response = await fetch(url, {
          headers: { Authorization: `Bearer ${token}` },
        });

        if (!response.ok) {
          throw new Error('Impossibile caricare il PDF.');
        }

        const buffer = await response.arrayBuffer();
        if (cancelled) return;

        const pdf = await pdfjsLib.getDocument({ data: buffer }).promise;
        if (cancelled || !containerRef.current) return;

        containerRef.current.innerHTML = '';

        for (let pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
          if (cancelled) break;
          const page = await pdf.getPage(pageNum);
          const viewport = page.getViewport({ scale: 1.4 });

          const wrapper = document.createElement('div');
          wrapper.className = 'pdf-canvas-wrapper';

          const canvas = document.createElement('canvas');
          canvas.width = viewport.width;
          canvas.height = viewport.height;

          const ctx = canvas.getContext('2d');
          if (!ctx) continue;

          wrapper.appendChild(canvas);
          containerRef.current.appendChild(wrapper);

          await page.render({ canvasContext: ctx, viewport }).promise;
        }

        setLoading(false);
      } catch (err) {
        if (!cancelled) {
          setError('Errore nel caricamento del PDF.');
          setLoading(false);
        }
      }
    };

    load();

    return () => {
      cancelled = true;
    };
  }, [url, token]);

  if (error) {
    return (
      <div className="pdf-loading" style={{ color: 'var(--error)', flexDirection: 'column', gap: '8px' }}>
        <span>⚠</span>
        <span>{error}</span>
      </div>
    );
  }

  return (
    <>
      {loading && (
        <div className="pdf-loading">
          <span className="spinner" style={{ marginRight: '10px' }} />
          Caricamento PDF…
        </div>
      )}
      <div
        ref={containerRef}
        className="pdf-viewer"
        style={{ display: loading ? 'none' : 'flex' }}
      />
    </>
  );
}
