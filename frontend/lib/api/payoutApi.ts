import { apiClient } from './client';
import type { PaymentMode } from './receiptApi';

export type PayoutStatus = 'PENDING' | 'PROCESSING' | 'SETTLED' | 'FAILED';

/** Settlement from broker to landlord: rent collected minus commission and deductions. */
export interface LandlordPayout {
  id: string;
  payoutNumber: string;
  organizationId: string;
  landlordId: string;
  landlordName?: string;
  propertyId?: string;
  totalCollected: number;
  commissionAmount: number;
  deductionsAmount: number;
  netPayoutAmount: number;
  payoutMode: PaymentMode;
  payoutStatus: PayoutStatus;
  utrNumber?: string;
  payoutDate?: string;
  notes?: string;
  statementPdfUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PayoutCreatePayload {
  landlordId: string;
  propertyId?: string;
  totalCollected: number;
  commissionAmount?: number;
  deductionsAmount?: number;
  /** Backend default: NET_BANKING */
  payoutMode?: PaymentMode;
  utrNumber?: string;
  /** ISO date (YYYY-MM-DD) */
  payoutDate?: string;
  notes?: string;
}

export const payoutApi = {
  getPayouts: async (): Promise<LandlordPayout[]> => {
    const response = await apiClient.get('/api/v1/landlord-payouts');
    return response.data.data;
  },

  getPayoutById: async (id: string): Promise<LandlordPayout> => {
    const response = await apiClient.get(`/api/v1/landlord-payouts/${id}`);
    return response.data.data;
  },

  getPayoutsByLandlord: async (landlordId: string): Promise<LandlordPayout[]> => {
    const response = await apiClient.get(`/api/v1/landlord-payouts/landlord/${landlordId}`);
    return response.data.data;
  },

  /** ADMIN / PROPERTY_MANAGER only. */
  createPayout: async (payload: PayoutCreatePayload): Promise<LandlordPayout> => {
    const response = await apiClient.post('/api/v1/landlord-payouts', payload);
    return response.data.data;
  },

  /**
   * ADMIN / PROPERTY_MANAGER only. Auto-calculates a payout from collections.
   * @param periodMonth optional month in `YYYY-MM` format (e.g. "2026-09"); defaults to the current month server-side.
   */
  calculateMonthlyPayout: async (landlordId: string, periodMonth?: string): Promise<LandlordPayout> => {
    const response = await apiClient.post(`/api/v1/landlord-payouts/calculate/${landlordId}`, null, {
      params: { periodMonth },
    });
    return response.data.data;
  },

  downloadPdf: async (id: string): Promise<Blob> => {
    const response = await apiClient.get(`/api/v1/landlord-payouts/${id}/pdf`, {
      responseType: 'blob',
    });
    return response.data;
  },
};
