import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(amount: number | string | undefined): string {
  if (amount === undefined || amount === null) return '₹0';
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(num);
}

export function getErrorMessage(error: unknown, fallback: string): string {
  if (error && typeof error === 'object') {
    const err = error as Record<string, any>;

    // 1. Check for structured API response bodies (Spring Boot / Axios)
    if (err.response?.data) {
      const data = err.response.data;
      if (typeof data === 'string' && data.trim()) {
        return data;
      }
      if (typeof data === 'object') {
        if (typeof data.message === 'string' && data.message.trim()) {
          return data.message;
        }
        if (typeof data.error === 'string' && data.error.trim()) {
          return data.error;
        }
        if (typeof data.detail === 'string' && data.detail.trim()) {
          return data.detail;
        }
        // Validation error maps e.g. { data: { pan: "Must be 10 characters" } }
        if (data.data && typeof data.data === 'object') {
          const values = Object.values(data.data);
          if (values.length > 0 && typeof values[0] === 'string' && values[0].trim()) {
            return values[0] as string;
          }
        }
      }
    }

    // 2. HTTP status code specific context
    if (err.response?.status) {
      const status = err.response.status;
      if (status === 401) return 'Session expired. Please sign in again.';
      if (status === 403) return 'Access denied. You do not have permission for this action.';
      if (status === 404) return `${fallback} (Resource not found)`;
      if (status === 502 || status === 503) {
        return `${fallback} (External service or provider currently unavailable)`;
      }
    }

    // 3. Network connection errors or client-side axios messages
    if (err.message && typeof err.message === 'string') {
      if (err.message === 'Network Error') {
        return `${fallback} (Network Error: Unable to connect to server)`;
      }
      if (err.code === 'ECONNABORTED' || err.message.toLowerCase().includes('timeout')) {
        return `${fallback} (Request timed out. Please try again)`;
      }
      if (!fallback.includes(err.message)) {
        return `${fallback}: ${err.message}`;
      }
    }
  }

  return fallback;
}

