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
  metadata?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TenantCreatePayload {
  userId?: string;
  fullName: string;
  email?: string;
  phone: string;
  permanentAddress?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelation?: string;
  idProofType?: string;
  idProofNumber?: string;
  idProofDocumentUrl?: string;
  propertyId?: string;
  unitId?: string;
  metadata?: string;
}

export interface TenantInvitePayload {
  fullName: string;
  phone: string;
  email: string;
  propertyId: string;
  propertyName: string;
  unitId?: string;
  unitNumber?: string;
  sharingType?: string;
  monthlyRent: number;
  securityDeposit: number;
  tokenPaid?: number;
  expectedMoveInDate?: string;
  notes?: string;
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

  inviteTenant: async (payload: TenantInvitePayload): Promise<{ tenant: Tenant; inviteLink: string; inviteToken: string }> => {
    const inviteToken = `inv_${Date.now().toString(36)}_${Math.random().toString(36).substring(2, 7)}`;
    const metadataObj = {
      inviteToken,
      propertyId: payload.propertyId,
      propertyName: payload.propertyName,
      unitId: payload.unitId,
      unitNumber: payload.unitNumber,
      sharingType: payload.sharingType,
      monthlyRent: payload.monthlyRent,
      securityDeposit: payload.securityDeposit,
      tokenPaid: payload.tokenPaid || 0,
      expectedMoveInDate: payload.expectedMoveInDate,
      notes: payload.notes,
      status: 'INVITED',
      invitedAt: new Date().toISOString(),
    };

    const tenant = await tenantApi.createTenant({
      fullName: payload.fullName,
      email: payload.email,
      phone: payload.phone,
      permanentAddress: 'Pending Self-KYC',
      propertyId: payload.propertyId,
      unitId: payload.unitId,
      metadata: JSON.stringify(metadataObj),
    });

    const origin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:3000';
    const params = new URLSearchParams({
      invite: inviteToken,
      propertyId: payload.propertyId,
      propertyName: payload.propertyName,
      unitId: payload.unitId || '',
      unitNumber: payload.unitNumber || '',
      sharingType: payload.sharingType || '',
      monthlyRent: String(payload.monthlyRent || 0),
      securityDeposit: String(payload.securityDeposit || 0),
      tokenPaid: String(payload.tokenPaid || 0),
      fullName: payload.fullName,
      phone: payload.phone,
      email: payload.email,
    });
    const inviteLink = `${origin}/kyc/tenant?${params.toString()}`;

    return { tenant, inviteLink, inviteToken };
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
