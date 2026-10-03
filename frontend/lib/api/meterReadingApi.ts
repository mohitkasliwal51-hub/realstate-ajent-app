import { apiClient } from './client';

export type MeterType = 'ELECTRICITY' | 'WATER' | 'GAS';

export interface MeterReading {
  id: string;
  organizationId: string;
  unitId: string;
  unitNumber?: string;
  meterType: MeterType;
  previousReading?: number;
  currentReading: number;
  unitsConsumed?: number;
  ratePerUnit: number;
  totalCharge?: number;
  readingDate: string;
  isBilled: boolean;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MeterReadingPayload {
  unitId: string;
  meterType?: MeterType;
  previousReading?: number;
  currentReading: number;
  ratePerUnit?: number;
  readingDate?: string;
  notes?: string;
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

  getMeterReadingsByLease: async (leaseId: string): Promise<MeterReading[]> => {
    const response = await apiClient.get(`/api/v1/meter-readings/lease/${leaseId}`);
    return response.data.data;
  },
};
