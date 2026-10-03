import { apiClient } from './client';

export type LeadSource = 'WHATSAPP' | 'WEBSITE' | 'NINETYNINE_ACRES' | 'NOBROKER' | 'MAGICBRICKS' | 'DIRECT';
export type LeadStatus = 'NEW' | 'CONTACTED' | 'VISITED' | 'CONVERTED' | 'LOST';

export interface PropertyLead {
  id: string;
  organizationId: string;
  propertyId?: string;
  propertyName?: string;
  unitId?: string;
  assignedToId?: string;
  name: string;
  phone: string;
  email?: string;
  source: LeadSource;
  status: LeadStatus;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LeadCreatePayload {
  /** Required: backend resolves the target organization from the property. */
  propertyId: string;
  /** Must belong to propertyId if provided. */
  unitId?: string;
  name: string;
  phone: string;
  email?: string;
  /** Backend default: WEBSITE */
  source?: LeadSource;
  notes?: string;
}

export const leadApi = {
  /** Public (no auth required). Property must be active. */
  createLead: async (payload: LeadCreatePayload): Promise<PropertyLead> => {
    const response = await apiClient.post('/api/v1/leads', payload);
    return response.data.data;
  },

  getLeadsByOrganization: async (): Promise<PropertyLead[]> => {
    const response = await apiClient.get('/api/v1/leads');
    return response.data.data;
  },

  updateLeadStatus: async (
    id: string,
    status: LeadStatus
  ): Promise<PropertyLead> => {
    const response = await apiClient.patch(`/api/v1/leads/${id}/status`, null, { params: { status } });
    return response.data.data;
  },
};
