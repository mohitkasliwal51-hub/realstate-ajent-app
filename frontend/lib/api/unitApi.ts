import { apiClient } from './client';

export type UnitStatus = 'AVAILABLE' | 'OCCUPIED' | 'RESERVED' | 'MAINTENANCE' | 'DISABLED';
export type SharingType = 'SINGLE' | 'DOUBLE' | 'TRIPLE' | 'FOUR_SHARING' | 'FULL_FLAT' | 'COMMERCIAL_SPACE';

export interface Unit {
  id: string;
  propertyId: string;
  organizationId: string;
  /** Parent room/flat when this unit is a bed inside a shared room. */
  parentUnitId?: string;
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
  propertyId: string;
  parentUnitId?: string;
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
    status?: UnitStatus
  ): Promise<Unit[]> => {
    const response = await apiClient.get(`/api/v1/properties/${propertyId}/units`, { params: { status } });
    return response.data.data;
  },

  getUnitById: async (id: string): Promise<Unit> => {
    const response = await apiClient.get(`/api/v1/units/${id}`);
    return response.data.data;
  },

  createUnit: async (payload: UnitCreatePayload): Promise<Unit> => {
    const response = await apiClient.post('/api/v1/units', payload);
    return response.data.data;
  },

  updateUnitStatus: async (
    id: string,
    status: UnitStatus
  ): Promise<Unit> => {
    const response = await apiClient.patch(
      `/api/v1/units/${id}/status`,
      null,
      { params: { status } }
    );
    return response.data.data;
  },

  /** Full replace: backend validates the same required fields as create. */
  updateUnit: async (
    id: string,
    payload: UnitCreatePayload
  ): Promise<Unit> => {
    const response = await apiClient.put(`/api/v1/units/${id}`, payload);
    return response.data.data;
  },

  deleteUnit: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/units/${id}`);
  },
};
