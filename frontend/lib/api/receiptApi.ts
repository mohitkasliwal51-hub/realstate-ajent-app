import { apiClient } from './client';

export type ReceiptType =
  | 'SECURITY_DEPOSIT'
  | 'RENT_PAYMENT'
  | 'UTILITY_BILL'
  | 'MAINTENANCE'
  | 'BROKERAGE_FEE'
  | 'AGREEMENT_FEE'
  | 'TOKEN_BOOKING'
  | 'OTHER';
export type PaymentMode = 'UPI' | 'NET_BANKING' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'CASH' | 'CHEQUE';

export interface Receipt {
  id: string;
  receiptNumber: string;
  organizationId: string;
  invoiceId?: string;
  leaseId: string;
  tenantId: string;
  receiptType: ReceiptType;
  amount: number;
  paymentMode: PaymentMode;
  transactionReference?: string;
  paymentDate: string;
  notes?: string;
  pdfUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReceiptCreatePayload {
  /** Link to an invoice to mark it (partially) paid. */
  invoiceId?: string;
  leaseId: string;
  tenantId: string;
  /** Backend default: RENT_PAYMENT */
  receiptType?: ReceiptType;
  amount: number;
  /** Backend default: UPI */
  paymentMode?: PaymentMode;
  transactionReference?: string;
  /** ISO date (YYYY-MM-DD). Backend default: today. */
  paymentDate?: string;
  notes?: string;
}

export const receiptApi = {
  getReceipts: async (): Promise<Receipt[]> => {
    const response = await apiClient.get('/api/v1/receipts');
    return response.data.data;
  },

  getReceiptById: async (id: string): Promise<Receipt> => {
    const response = await apiClient.get(`/api/v1/receipts/${id}`);
    return response.data.data;
  },

  getMyReceipts: async (): Promise<Receipt[]> => {
    const response = await apiClient.get('/api/v1/receipts/me');
    return response.data.data;
  },

  createReceipt: async (payload: ReceiptCreatePayload): Promise<Receipt> => {
    const response = await apiClient.post('/api/v1/receipts', payload);
    return response.data.data;
  },

  downloadPdf: async (id: string): Promise<Blob> => {
    const response = await apiClient.get(`/api/v1/receipts/${id}/pdf`, {
      responseType: 'blob',
    });
    return response.data;
  },
};
