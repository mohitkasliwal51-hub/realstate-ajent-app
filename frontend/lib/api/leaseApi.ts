import { apiClient } from './client';

export type LeaseStatus = 'DRAFT' | 'PENDING_ESIGN' | 'ACTIVE' | 'EXPIRED' | 'TERMINATED' | 'CANCELLED';
export type BrokerageFeeType = 'NONE' | 'ONE_TIME' | 'RECURRING_PERCENTAGE' | 'RECURRING_FIXED';
export type MaintenanceFeeType = 'NONE' | 'MONTHLY' | 'ANNUAL_ONE_TIME';

export interface Lease {
  id: string;
  organizationId: string;
  unitId: string;
  tenantId: string;
  /** Null for self-owned properties (owner mode). */
  landlordId?: string;
  landlordName?: string;
  createdById?: string;
  agreementTemplateId?: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  securityDeposit: number;
  rentDueDay: number;
  noticePeriodDays: number;
  lockInPeriodMonths: number;
  brokerageFeeType: BrokerageFeeType;
  brokerageAmount?: number;
  maintenanceFeeType: MaintenanceFeeType;
  maintenanceFeeAmount?: number;
  agreementFeeAmount?: number;
  customClauses?: string;
  termsAndConditions?: string;
  status: LeaseStatus;
  agreementPdfUrl?: string;
  isEsignCompleted: boolean;
  esignTransactionId?: string;
  eStampNumber?: string;
  estampNumber?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LeaseCreatePayload {
  unitId: string;
  tenantId: string;
  /** Broker mode: should match the property's landlord (backend auto-fills if omitted). */
  landlordId?: string;
  agreementTemplateId?: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  securityDeposit: number;
  /** Backend default: 5 */
  rentDueDay?: number;
  /** Backend default: 30 */
  noticePeriodDays?: number;
  /** Backend default: 6 */
  lockInPeriodMonths?: number;
  /** Backend default: NONE. Broker mode only. */
  brokerageFeeType?: BrokerageFeeType;
  brokerageAmount?: number;
  /** Backend default: NONE */
  maintenanceFeeType?: MaintenanceFeeType;
  maintenanceFeeAmount?: number;
  agreementFeeAmount?: number;
  customClauses?: string;
  termsAndConditions?: string;
  status: LeaseStatus;
}

export const leaseApi = {
  getLeases: async (): Promise<Lease[]> => {
    const response = await apiClient.get('/api/v1/leases');
    return response.data.data;
  },

  getLeaseById: async (id: string): Promise<Lease> => {
    const response = await apiClient.get(`/api/v1/leases/${id}`);
    return response.data.data;
  },

  getMyLease: async (): Promise<Lease> => {
    const response = await apiClient.get('/api/v1/leases/me');
    return response.data.data;
  },

  createLease: async (payload: LeaseCreatePayload): Promise<Lease> => {
    const response = await apiClient.post('/api/v1/leases', payload);
    return response.data.data;
  },

  /**
   * Allowed transitions: DRAFT → PENDING_ESIGN | ACTIVE | CANCELLED,
   * PENDING_ESIGN → ACTIVE | CANCELLED, ACTIVE → EXPIRED | TERMINATED.
   */
  updateLeaseStatus: async (
    id: string,
    status: LeaseStatus
  ): Promise<Lease> => {
    const response = await apiClient.patch(`/api/v1/leases/${id}/status`, null, { params: { status } });
    return response.data.data;
  },

  downloadPdf: async (id: string): Promise<Blob> => {
    const response = await apiClient.get(`/api/v1/leases/${id}/pdf`, {
      responseType: 'blob',
    });
    return response.data;
  },
};

/** Valid next statuses for a lease, mirroring backend validateStatusTransition. */
export const LEASE_STATUS_TRANSITIONS: Record<LeaseStatus, LeaseStatus[]> = {
  DRAFT: ['PENDING_ESIGN', 'ACTIVE', 'CANCELLED'],
  PENDING_ESIGN: ['ACTIVE', 'CANCELLED'],
  ACTIVE: ['EXPIRED', 'TERMINATED'],
  EXPIRED: [],
  TERMINATED: [],
  CANCELLED: [],
};
