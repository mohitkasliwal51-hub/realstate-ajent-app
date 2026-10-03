import { apiClient } from './client';

export type InvoiceType = 'MOVE_IN' | 'MONTHLY_RENT' | 'UTILITY_ONLY' | 'MAINTENANCE_ONLY' | 'FINAL_SETTLEMENT';
export type InvoiceStatus = 'DRAFT' | 'UNPAID' | 'PARTIAL' | 'PAID' | 'OVERDUE' | 'CANCELLED';
export type ChargeType =
  | 'RENT'
  | 'SECURITY_DEPOSIT'
  | 'ONE_TIME_BROKERAGE'
  | 'RECURRING_COMMISSION'
  | 'MAINTENANCE_FEE'
  | 'AGREEMENT_FEE'
  | 'ELECTRICITY_BILL'
  | 'WATER_BILL'
  | 'LATE_FEE'
  | 'TOKEN_BOOKING'
  | 'OTHER';

export interface InvoiceLineItem {
  id: string;
  invoiceId: string;
  chargeType: ChargeType;
  description?: string;
  quantity?: number;
  unitPrice?: number;
  amount: number;
  createdAt?: string;
}

export interface Invoice {
  id: string;
  organizationId: string;
  invoiceNumber: string;
  leaseId: string;
  tenantId: string;
  unitId: string;
  invoiceType: InvoiceType;
  billingPeriodStart: string;
  billingPeriodEnd: string;
  dueDate: string;
  subtotalAmount: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  paidAmount: number;
  balanceDue: number;
  status: InvoiceStatus;
  invoicePdfUrl?: string;
  notes?: string;
  lineItems: InvoiceLineItem[];
  createdAt?: string;
  updatedAt?: string;
}

export interface InvoiceLineItemPayload {
  chargeType: ChargeType;
  description?: string;
  quantity?: number;
  unitPrice?: number;
  amount?: number;
}

export interface InvoiceCreatePayload {
  leaseId: string;
  tenantId: string;
  unitId: string;
  /** Backend default: MONTHLY_RENT */
  invoiceType?: InvoiceType;
  /** ISO dates (YYYY-MM-DD) */
  billingPeriodStart: string;
  billingPeriodEnd: string;
  dueDate: string;
  taxAmount?: number;
  discountAmount?: number;
  notes?: string;
  lineItems?: InvoiceLineItemPayload[];
}

export const invoiceApi = {
  getInvoices: async (): Promise<Invoice[]> => {
    const response = await apiClient.get('/api/v1/invoices');
    return response.data.data;
  },

  getInvoiceById: async (id: string): Promise<Invoice> => {
    const response = await apiClient.get(`/api/v1/invoices/${id}`);
    return response.data.data;
  },

  getInvoicesByLease: async (leaseId: string): Promise<Invoice[]> => {
    const response = await apiClient.get(`/api/v1/invoices/lease/${leaseId}`);
    return response.data.data;
  },

  /** ADMIN / PROPERTY_MANAGER only. */
  createInvoice: async (payload: InvoiceCreatePayload): Promise<Invoice> => {
    const response = await apiClient.post('/api/v1/invoices', payload);
    return response.data.data;
  },

  /** ADMIN / PROPERTY_MANAGER only. Builds deposit + first rent (+ brokerage/agreement fees) from the lease. */
  generateMoveInInvoice: async (leaseId: string): Promise<Invoice> => {
    const response = await apiClient.post(`/api/v1/invoices/move-in/${leaseId}`);
    return response.data.data;
  },

  /** ADMIN / PROPERTY_MANAGER only. Generates monthly invoices for all active leases. */
  generateMonthlyInvoices: async (): Promise<Invoice[]> => {
    const response = await apiClient.post('/api/v1/invoices/generate-monthly');
    return response.data.data;
  },

  downloadPdf: async (id: string): Promise<Blob> => {
    const response = await apiClient.get(`/api/v1/invoices/${id}/pdf`, {
      responseType: 'blob',
    });
    return response.data;
  },
};
