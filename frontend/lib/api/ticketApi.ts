import { apiClient } from './client';

export interface MaintenanceTicket {
  id: string;
  organizationId: string;
  unitId: string;
  unitNumber: string;
  tenantId: string;
  tenantName: string;
  title: string;
  description?: string;
  category: string;
  priority: string;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TicketCreatePayload {
  organizationId: string;
  unitId: string;
  tenantId: string;
  title: string;
  description?: string;
  category?: string;
  priority?: string;
}

export const ticketApi = {
  createTicket: async (payload: TicketCreatePayload): Promise<MaintenanceTicket> => {
    const response = await apiClient.post('/api/v1/tickets', payload);
    return response.data.data;
  },

  getTicketsByOrganization: async (organizationId: string): Promise<MaintenanceTicket[]> => {
    const response = await apiClient.get('/api/v1/tickets', {
      params: { organizationId },
    });
    return response.data.data;
  },

  getTicketsByTenant: async (tenantId: string, organizationId: string): Promise<MaintenanceTicket[]> => {
    const response = await apiClient.get('/api/v1/tickets/tenant', {
      params: { tenantId, organizationId },
    });
    return response.data.data;
  },

  updateTicketStatus: async (
    id: string,
    organizationId: string,
    status: string
  ): Promise<MaintenanceTicket> => {
    const response = await apiClient.patch(`/api/v1/tickets/${id}/status`, null, {
      params: { organizationId, status },
    });
    return response.data.data;
  },
};
