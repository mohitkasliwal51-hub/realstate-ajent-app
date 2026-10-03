'use client';

import React from 'react';
import { AlertTriangle, Trash2 } from 'lucide-react';
import { Modal } from './Modal';
import { Button } from './Button';

interface ConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void | Promise<void>;
  title: string;
  /** What exactly will happen. Be specific, e.g. "Unit A-101 will be deleted." */
  description: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'danger' | 'primary';
  isLoading?: boolean;
  /** Optional extra consequence shown in a highlighted box (danger variant only). */
  warning?: string;
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  description,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  variant = 'danger',
  isLoading = false,
  warning,
}) => {
  return (
    <Modal isOpen={isOpen} onClose={isLoading ? () => undefined : onClose} title={title} description={description}>
      <div className="space-y-4 pt-2">
        {variant === 'danger' && warning && (
          <div className="flex items-start gap-3 p-3.5 rounded-xl bg-red-50 border border-red-100">
            <AlertTriangle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
            <p className="text-xs font-medium text-red-800">{warning}</p>
          </div>
        )}

        <div className="flex items-center justify-end gap-3 pt-2">
          <Button type="button" variant="outline" onClick={onClose} disabled={isLoading}>
            {cancelText}
          </Button>
          <Button
            type="button"
            variant={variant === 'danger' ? 'danger' : 'primary'}
            onClick={onConfirm}
            isLoading={isLoading}
          >
            {variant === 'danger' && <Trash2 className="w-4 h-4 mr-1.5" />}
            {confirmText}
          </Button>
        </div>
      </div>
    </Modal>
  );
};
