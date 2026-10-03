import { apiClient } from './client';

export type OwnerType = 'INDIVIDUAL' | 'COMPANY' | 'TRUST' | 'PARTNERSHIP';

/** A property owner managed by this organization (broker / hybrid mode). */
export interface Landlord {
  id: string;
  managingOrganizationId: string;
  profileId?: string;
  ownerType: OwnerType;
  legalName: string;
  email?: string;
  phone: string;
  pan?: string;
  gstin?: string;
  address?: string;
  bankAccountNumber?: string;
  bankIfscCode?: string;
  bankName?: string;
  accountHolderName?: string;
  ownerUpiId?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface LandlordPayload {
  profileId?: string;
  /** Backend default: INDIVIDUAL */
  ownerType?: OwnerType;
  legalName: string;
  email?: string;
  phone: string;
  pan?: string;
  gstin?: string;
  address?: string;
  bankAccountNumber?: string;
  bankIfscCode?: string;
  bankName?: string;
  accountHolderName?: string;
  ownerUpiId?: string;
}

export const landlordApi = {
  getLandlords: async (): Promise<Landlord[]> => {
    const response = await apiClient.get('/api/v1/landlords');
    return response.data.data;
  },

  getLandlordById: async (id: string): Promise<Landlord> => {
    const response = await apiClient.get(`/api/v1/landlords/${id}`);
    return response.data.data;
  },

  /** ADMIN / PROPERTY_MANAGER only. */
  createLandlord: async (payload: LandlordPayload): Promise<Landlord> => {
    const response = await apiClient.post('/api/v1/landlords', payload);
    return response.data.data;
  },

  /** ADMIN / PROPERTY_MANAGER only. Full replace. */
  updateLandlord: async (id: string, payload: LandlordPayload): Promise<Landlord> => {
    const response = await apiClient.put(`/api/v1/landlords/${id}`, payload);
    return response.data.data;
  },

  deleteLandlord: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/landlords/${id}`);
  },
};
