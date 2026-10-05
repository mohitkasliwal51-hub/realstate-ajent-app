import { apiClient } from './client';

export type MeterType = 'ELECTRICITY' | 'WATER' | 'GAS';
export type MeterScope = 'MASTER_DISCOM' | 'HALL_AC' | 'ROOM_AC' | 'COMMON_AREA';
export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'DISPUTED';

export interface MeterReading {
  id: string;
  organizationId: string;
  propertyId?: string;
  unitId?: string;
  unitNumber?: string;
  meterType: MeterType;
  meterScope?: MeterScope;
  previousReading?: number;
  currentReading: number;
  unitsConsumed?: number;
  ratePerUnit: number;
  totalCharge?: number;
  readingDate: string;
  isBilled: boolean;
  notes?: string;
  photoUrl?: string;
  submittedByUserId?: string;
  verificationStatus?: VerificationStatus;
  disputeReason?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MeterReadingPayload {
  propertyId?: string;
  unitId?: string;
  meterType?: MeterType;
  meterScope?: MeterScope;
  previousReading?: number;
  currentReading: number;
  ratePerUnit?: number;
  readingDate?: string;
  notes?: string;
  photoUrl?: string;
}

export const meterReadingApi = {
  getMeterReadings: async (): Promise<MeterReading[]> => {
    const response = await apiClient.get('/api/v1/meter-readings');
    return response.data.data;
  },

  createMeterReading: async (payload: MeterReadingPayload): Promise<MeterReading> => {
    const response = await apiClient.post('/api/v1/meter-readings', payload);
    return response.data.data;
  },

  getMeterReadingById: async (id: string): Promise<MeterReading> => {
    const response = await apiClient.get(`/api/v1/meter-readings/${id}`);
    return response.data.data;
  },

  getMeterReadingsByUnit: async (unitId: string): Promise<MeterReading[]> => {
    const response = await apiClient.get(`/api/v1/meter-readings/unit/${unitId}`);
    return response.data.data;
  },

  getMeterReadingsByProperty: async (propertyId: string): Promise<MeterReading[]> => {
    const response = await apiClient.get(`/api/v1/meter-readings/property/${propertyId}`);
    return response.data.data;
  },

  getMeterReadingsByLease: async (leaseId: string): Promise<MeterReading[]> => {
    const response = await apiClient.get(`/api/v1/meter-readings/lease/${leaseId}`);
    return response.data.data;
  },

  verifyMeterReading: async (id: string, status: VerificationStatus, disputeReason?: string): Promise<MeterReading> => {
    const response = await apiClient.patch(`/api/v1/meter-readings/${id}/verify`, null, {
      params: { status, disputeReason }
    });
    return response.data.data;
  },

  runUtilitySplit: async (propertyId: string, billingMonth?: string): Promise<any[]> => {
    const response = await apiClient.post(`/api/v1/meter-readings/split/property/${propertyId}`, null, {
      params: billingMonth ? { billingMonth } : undefined
    });
    return response.data.data;
  },
};
