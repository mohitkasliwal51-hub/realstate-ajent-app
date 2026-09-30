import { apiClient } from './client';

export type ReceiptType = 'SECURITY_DEPOSIT' | 'RENT_PAYMENT' | 'UTILITY_BILL' | 'MAINTENANCE' | 'TOKEN_BOOKING' | 'OTHER';
export type PaymentMode = 'UPI' | 'BANK_TRANSFER' | 'CASH' | 'CHEQUE' | 'RAZORPAY_ONLINE' | 'OTHER';

export interface Receipt {
  id: string;
  organizationId: string;
  leaseId: string;
  tenantId: string;
  receiptNumber: string;
  type: ReceiptType;
  amount: number;
  paymentMode: PaymentMode;
  transactionRef?: string;
  paymentDate: string;
  notes?: string;
  receiptPdfUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReceiptCreatePayload {
  organizationId: string;
  ownerId: string;
  leaseId: string;
  tenantId: string;
  type: ReceiptType;
  amount: number;
  paymentMode: PaymentMode;
  transactionRef?: string;
  notes?: string;
}

export const receiptApi = {
  getReceipts: async (organizationId: string): Promise<Receipt[]> => {
    const response = await apiClient.get('/api/v1/receipts', {
      params: { organizationId },
    });
    return response.data.data;
  },

  getReceiptById: async (id: string, organizationId: string): Promise<Receipt> => {
    const response = await apiClient.get(`/api/v1/receipts/${id}`, {
      params: { organizationId },
    });
    return response.data.data;
  },

  createReceipt: async (payload: ReceiptCreatePayload): Promise<Receipt> => {
    const response = await apiClient.post('/api/v1/receipts', payload);
    return response.data.data;
  },

  downloadPdf: async (id: string, organizationId: string): Promise<Blob> => {
    const response = await apiClient.get(`/api/v1/receipts/${id}/pdf`, {
      params: { organizationId },
      responseType: 'blob',
    });
    return response.data;
  },
};
