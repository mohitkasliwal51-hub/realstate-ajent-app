import { apiClient } from './client';

export type LeaseStatus = 'DRAFT' | 'PENDING_ESIGN' | 'ACTIVE' | 'EXPIRED' | 'TERMINATED' | 'CANCELLED';

export interface Lease {
  id: string;
  organizationId: string;
  unitId: string;
  tenantId: string;
  ownerId: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  securityDeposit: number;
  status: LeaseStatus;
  isEsignCompleted: boolean;
  esignTransactionId?: string;
  eStampNumber?: string;
  customClauses?: string;
  termsAndConditions?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LeaseCreatePayload {
  organizationId: string;
  unitId: string;
  tenantId: string;
  ownerId: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  securityDeposit: number;
  agreementTemplateId?: string;
  customClauses?: string;
  termsAndConditions?: string;
  status?: LeaseStatus;
}

export const leaseApi = {
  getLeases: async (organizationId: string): Promise<Lease[]> => {
    const response = await apiClient.get('/api/v1/leases', {
      params: { organizationId },
    });
    return response.data.data;
  },

  getLeaseById: async (id: string, organizationId: string): Promise<Lease> => {
    const response = await apiClient.get(`/api/v1/leases/${id}`, {
      params: { organizationId },
    });
    return response.data.data;
  },

  createLease: async (payload: LeaseCreatePayload): Promise<Lease> => {
    const response = await apiClient.post('/api/v1/leases', payload);
    return response.data.data;
  },

  updateLeaseStatus: async (
    id: string,
    organizationId: string,
    status: LeaseStatus
  ): Promise<Lease> => {
    const response = await apiClient.patch(`/api/v1/leases/${id}/status`, null, {
      params: { organizationId, status },
    });
    return response.data.data;
  },

  downloadPdf: async (id: string, organizationId: string): Promise<Blob> => {
    const response = await apiClient.get(`/api/v1/leases/${id}/pdf`, {
      params: { organizationId },
      responseType: 'blob',
    });
    return response.data;
  },
};
