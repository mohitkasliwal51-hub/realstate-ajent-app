import { apiClient } from './client';

export type LeadSource = 'WHATSAPP' | 'WEBSITE' | 'NINETYNINE_ACRES' | 'NOBROKER' | 'MAGICBRICKS' | 'DIRECT';
export type LeadStatus = 'NEW' | 'CONTACTED' | 'VISITED' | 'CONVERTED' | 'LOST';

export interface PropertyLead {
  id: string;
  organizationId: string;
  propertyId?: string;
  propertyName?: string;
  unitId?: string;
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
  organizationId: string;
  propertyId?: string;
  unitId?: string;
  name: string;
  phone: string;
  email?: string;
  source?: LeadSource;
  notes?: string;
}

export const leadApi = {
  createLead: async (payload: LeadCreatePayload): Promise<PropertyLead> => {
    const response = await apiClient.post('/api/v1/leads', payload);
    return response.data.data;
  },

  getLeadsByOrganization: async (organizationId: string): Promise<PropertyLead[]> => {
    const response = await apiClient.get('/api/v1/leads', {
      params: { organizationId },
    });
    return response.data.data;
  },

  updateLeadStatus: async (
    id: string,
    organizationId: string,
    status: LeadStatus
  ): Promise<PropertyLead> => {
    const response = await apiClient.patch(`/api/v1/leads/${id}/status`, null, {
      params: { organizationId, status },
    });
    return response.data.data;
  },
};
