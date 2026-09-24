import { useState, useRef, DragEvent } from 'react';

interface Props {
  onFile: (file: File) => void;
  loading: boolean;
  description: string;
}

export default function DocumentUpload({ onFile, loading, description }: Props) {
  const [dragOver, setDragOver] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFile = (file: File) => {
    setSelectedFile(file);
    onFile(file);
  };

  const onDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  };

  const onDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragOver(true);
  };

  if (selectedFile && loading) {
    return (
      <div className="file-selected">
        <span>📎</span>
        <span className="file-selected-name">{selectedFile.name}</span>
        <span className="spinner" />
      </div>
    );
  }

  if (selectedFile && !loading) {
    return (
      <div className="file-selected">
        <span style={{ color: 'var(--success)' }}>✓</span>
        <span className="file-selected-name">{selectedFile.name}</span>
        <button
          className="btn btn-ghost"
          style={{ padding: '4px 10px', fontSize: '12px' }}
          onClick={() => {
            setSelectedFile(null);
            if (inputRef.current) inputRef.current.value = '';
          }}
        >
          Cambia
        </button>
      </div>
    );
  }

  return (
    <div
      className={`drop-area ${dragOver ? 'drag-over' : ''}`}
      onDrop={onDrop}
      onDragOver={onDragOver}
      onDragLeave={() => setDragOver(false)}
      onClick={() => inputRef.current?.click()}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && inputRef.current?.click()}
    >
      <input
        ref={inputRef}
        type="file"
        accept=".pdf,.png,.jpg,.jpeg"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) handleFile(file);
        }}
      />
      <div className="drop-area-icon">📂</div>
      <p>{description || 'Trascina il documento qui'}</p>
      <small>PDF, PNG, JPG supportati</small>
    </div>
  );
}
