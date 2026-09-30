import { apiClient } from './client';

export interface Tenant {
  id: string;
  organizationId: string;
  fullName: string;
  email: string;
  phone: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  permanentAddress?: string;
  idProofType?: string;
  idProofLast4?: string;
  isIdVerified: boolean;
  idProofFrontUrl?: string;
  idProofBackUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TenantCreatePayload {
  organizationId: string;
  ownerId: string;
  fullName: string;
  email: string;
  phone: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  permanentAddress?: string;
  idProofType?: string;
  idProofNumber?: string;
}

export const tenantApi = {
  getTenants: async (organizationId: string): Promise<Tenant[]> => {
    const response = await apiClient.get('/api/v1/tenants', {
      params: { organizationId },
    });
    return response.data.data;
  },

  getTenantById: async (id: string, organizationId: string): Promise<Tenant> => {
    const response = await apiClient.get(`/api/v1/tenants/${id}`, {
      params: { organizationId },
    });
    return response.data.data;
  },

  createTenant: async (payload: TenantCreatePayload): Promise<Tenant> => {
    const response = await apiClient.post('/api/v1/tenants', payload);
    return response.data.data;
  },
};
