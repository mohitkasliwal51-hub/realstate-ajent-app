import { apiClient } from './client';

export type UnitStatus = 'AVAILABLE' | 'OCCUPIED' | 'RESERVED' | 'MAINTENANCE' | 'DISABLED';
export type SharingType = 'SINGLE' | 'DOUBLE' | 'TRIPLE' | 'FOUR_SHARING' | 'FULL_FLAT' | 'CUSTOM';

export interface Unit {
  id: string;
  propertyId: string;
  organizationId: string;
  unitNumber: string;
  floorNumber: number;
  sharingType: SharingType;
  monthlyRent: number;
  securityDeposit: number;
  status: UnitStatus;
  currentLeaseId?: string;
  amenities?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface UnitCreatePayload {
  organizationId: string;
  propertyId: string;
  unitNumber: string;
  floorNumber?: number;
  sharingType?: SharingType;
  monthlyRent: number;
  securityDeposit: number;
  status?: UnitStatus;
  amenities?: string;
  notes?: string;
}

export const unitApi = {
  getUnitsByProperty: async (
    propertyId: string,
    organizationId: string,
    status?: UnitStatus
  ): Promise<Unit[]> => {
    const response = await apiClient.get(`/api/v1/properties/${propertyId}/units`, {
      params: { organizationId, status },
    });
    return response.data.data;
  },

  getUnitById: async (id: string, organizationId: string): Promise<Unit> => {
    const response = await apiClient.get(`/api/v1/units/${id}`, {
      params: { organizationId },
    });
    return response.data.data;
  },

  createUnit: async (payload: UnitCreatePayload): Promise<Unit> => {
    const response = await apiClient.post('/api/v1/units', payload);
    return response.data.data;
  },

  updateUnitStatus: async (
    id: string,
    organizationId: string,
    status: UnitStatus
  ): Promise<Unit> => {
    const response = await apiClient.patch(
      `/api/v1/units/${id}/status`,
      null,
      { params: { organizationId, status } }
    );
    return response.data.data;
  },

  updateUnit: async (
    id: string,
    organizationId: string,
    payload: UnitCreatePayload
  ): Promise<Unit> => {
    const response = await apiClient.put(`/api/v1/units/${id}`, payload, {
      params: { organizationId },
    });
    return response.data.data;
  },
};
