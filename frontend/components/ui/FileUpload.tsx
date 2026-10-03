'use client';

import React, { useRef, useState } from 'react';
import { UploadCloud, FileText, CheckCircle2, X, Loader2, ExternalLink } from 'lucide-react';
import {
  fileApi,
  FileCategory,
  ACCEPT_BY_CATEGORY,
  MAX_UPLOAD_BYTES,
  isPrivateFileUrl,
  resolveFileUrl,
} from '@/lib/api/fileApi';
import { getErrorMessage } from '@/lib/utils';

interface FileUploadProps {
  label?: string;
  /** Decides what is accepted and whether the file is public or private (server enforces both). */
  category: FileCategory;
  /** Stored URL. Pass '' to use the component as an "add another" dropzone. */
  value?: string;
  onChange: (url: string) => void;
  placeholder?: string;
  className?: string;
  disabled?: boolean;
}

const IMAGE_EXT = /\.(jpe?g|png|webp)$/i;

export const FileUpload: React.FC<FileUploadProps> = ({
  label,
  category,
  value,
  onChange,
  placeholder = 'Drag & drop or click to upload',
  className = '',
  disabled = false,
}) => {
  const [isUploading, setIsUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const accept = ACCEPT_BY_CATEGORY[category];
  const allowedTypes = accept.split(',');
  const isPdfAllowed = allowedTypes.includes('application/pdf');
  const hint = `${isPdfAllowed ? 'JPG, PNG, WEBP or PDF' : 'JPG, PNG or WEBP'} up to ${MAX_UPLOAD_BYTES / (1024 * 1024)}MB`;

  const handleUpload = async (file: File) => {
    setError(null);
    if (!allowedTypes.includes(file.type)) {
      setError(`Unsupported file type. Allowed: ${hint}.`);
      return;
    }
    if (file.size > MAX_UPLOAD_BYTES) {
      setError(`File is too large. Maximum is ${MAX_UPLOAD_BYTES / (1024 * 1024)}MB.`);
      return;
    }
    setIsUploading(true);
    try {
      const res = await fileApi.uploadFile(file, category);
      onChange(res.url);
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to upload file'));
    } finally {
      setIsUploading(false);
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  const onDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (disabled) return;
    setDragActive(e.type === 'dragenter' || e.type === 'dragover');
  };

  const onDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (disabled) return;
    const file = e.dataTransfer.files?.[0];
    if (file) void handleUpload(file);
  };

  const onPick = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) void handleUpload(file);
  };

  const handleView = async () => {
    if (!value) return;
    try {
      await fileApi.openFile(value);
    } catch (err) {
      setError(getErrorMessage(err, 'Could not open file'));
    }
  };

  // Only public images can be previewed inline: private files need the bearer token.
  const canPreview = !!value && !isPrivateFileUrl(value) && IMAGE_EXT.test(value);

  return (
    <div className={`w-full flex flex-col gap-1.5 ${className}`}>
      {label && (
        <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">{label}</label>
      )}

      {value ? (
        <div className="border border-slate-200 rounded-xl p-3 bg-slate-50 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 overflow-hidden">
            {canPreview ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={resolveFileUrl(value)}
                alt="Uploaded file"
                className="w-10 h-10 object-cover rounded-lg border border-slate-200"
              />
            ) : (
              <div className="w-10 h-10 rounded-lg bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600">
                <FileText className="w-5 h-5" />
              </div>
            )}
            <div className="flex flex-col truncate">
              <span className="text-xs font-semibold text-slate-800 truncate">
                {decodeURIComponent(value.split('/').pop()?.split('?')[0] || 'Uploaded file')}
              </span>
              <span className="text-[10px] text-emerald-600 flex items-center gap-1 font-medium">
                <CheckCircle2 className="w-3 h-3" /> Uploaded
                {isPrivateFileUrl(value) ? ' (private)' : ''}
              </span>
            </div>
          </div>
          <div className="flex items-center gap-1 shrink-0">
            <button
              type="button"
              onClick={handleView}
              className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition-colors"
              title="View file"
            >
              <ExternalLink className="w-4 h-4" />
            </button>
            {!disabled && (
              <button
                type="button"
                onClick={() => onChange('')}
                className="p-1.5 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                title="Remove file"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      ) : (
        <div
          onDragEnter={onDrag}
          onDragLeave={onDrag}
          onDragOver={onDrag}
          onDrop={onDrop}
          onClick={() => !disabled && inputRef.current?.click()}
          className={`border-2 border-dashed rounded-xl p-4 text-center transition-all flex flex-col items-center justify-center gap-2 ${
            disabled ? 'opacity-60 cursor-not-allowed' : 'cursor-pointer'
          } ${dragActive ? 'border-blue-500 bg-blue-50/50' : 'border-slate-300 hover:border-slate-400 bg-white'}`}
        >
          <input
            ref={inputRef}
            type="file"
            accept={accept}
            onChange={onPick}
            className="hidden"
            disabled={disabled}
          />
          {isUploading ? (
            <div className="flex flex-col items-center gap-2 py-2">
              <Loader2 className="w-6 h-6 text-blue-600 animate-spin" />
              <span className="text-xs font-medium text-slate-600">Uploading...</span>
            </div>
          ) : (
            <>
              <div className="p-2.5 rounded-full bg-slate-100 text-slate-600">
                <UploadCloud className="w-5 h-5" />
              </div>
              <div className="space-y-0.5">
                <p className="text-xs font-semibold text-slate-700">{placeholder}</p>
                <p className="text-[10px] text-slate-400">{hint}</p>
              </div>
            </>
          )}
        </div>
      )}

      {error && <p className="text-[11px] text-red-600 font-medium">{error}</p>}
    </div>
  );
};
