import { apiClient } from './client';

export type VerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export interface Tenant {
  id: string;
  organizationId: string;
  /** Linked login profile (TENANT role), if the tenant has portal access. */
  userId?: string;
  fullName: string;
  email?: string;
  phone: string;
  permanentAddress?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelation?: string;
  idProofType?: string;
  /** Backend only returns the last 4 digits; the full number is never sent back. */
  idProofLast4?: string;
  idProofDocumentUrl?: string;
  kycStatus: VerificationStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface TenantCreatePayload {
  userId?: string;
  fullName: string;
  email?: string;
  phone: string;
  /** Required by backend (@NotBlank). */
  permanentAddress: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelation?: string;
  idProofType?: string;
  idProofNumber?: string;
  idProofDocumentUrl?: string;
}

export const tenantApi = {
  getTenants: async (): Promise<Tenant[]> => {
    const response = await apiClient.get('/api/v1/tenants');
    return response.data.data;
  },

  getTenantById: async (id: string): Promise<Tenant> => {
    const response = await apiClient.get(`/api/v1/tenants/${id}`);
    return response.data.data;
  },

  createTenant: async (payload: TenantCreatePayload): Promise<Tenant> => {
    const response = await apiClient.post('/api/v1/tenants', payload);
    return response.data.data;
  },

  updateKycStatus: async (
    id: string,
    status: VerificationStatus
  ): Promise<Tenant> => {
    const response = await apiClient.patch(`/api/v1/tenants/${id}/kyc`, null, { params: { status } });
    return response.data.data;
  },

  getMyProfile: async (): Promise<Tenant> => {
    const response = await apiClient.get('/api/v1/tenants/me');
    return response.data.data;
  },
};
