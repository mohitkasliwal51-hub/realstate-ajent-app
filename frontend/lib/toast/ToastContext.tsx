'use client';

import React, { createContext, useContext, useState, useCallback } from 'react';
import { ToastContainer, ToastMessage, ToastType } from '@/components/ui/Toast';

interface ToastContextType {
  toast: {
    success: (message: string, title?: string) => void;
    error: (message: string, title?: string) => void;
    warning: (message: string, title?: string) => void;
    info: (message: string, title?: string) => void;
  };
  showToast: (type: ToastType, message: string, title?: string, duration?: number) => void;
  removeToast: (id: string) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (type: ToastType, message: string, title?: string, duration = 4000) => {
      const id = `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
      const newToast: ToastMessage = { id, type, message, title, duration };
      setToasts((prev) => [...prev, newToast]);
    },
    []
  );

  const toast = {
    success: useCallback((message: string, title?: string) => showToast('success', message, title), [showToast]),
    error: useCallback((message: string, title?: string) => showToast('error', message, title, 5000), [showToast]),
    warning: useCallback((message: string, title?: string) => showToast('warning', message, title), [showToast]),
    info: useCallback((message: string, title?: string) => showToast('info', message, title), [showToast]),
  };

  return (
    <ToastContext.Provider value={{ toast, showToast, removeToast }}>
      {children}
      <ToastContainer toasts={toasts} onDismiss={removeToast} />
    </ToastContext.Provider>
  );
};

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};
