import { apiClient } from './client';

export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

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
  idProofDocumentUrl?: string;
  kycStatus: VerificationStatus;
  idProofFrontUrl?: string;
  idProofBackUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TenantCreatePayload {
  organizationId: string;
  fullName: string;
  email: string;
  phone: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelation?: string;
  permanentAddress?: string;
  idProofType?: string;
  idProofNumber?: string;
  idProofDocumentUrl?: string;
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

  updateKycStatus: async (
    id: string,
    organizationId: string,
    status: VerificationStatus
  ): Promise<Tenant> => {
    const response = await apiClient.patch(`/api/v1/tenants/${id}/kyc`, null, {
      params: { organizationId, status },
    });
    return response.data.data;
  },
};
