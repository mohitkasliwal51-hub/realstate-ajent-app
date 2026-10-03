import { apiClient } from './client';

/**
 * Backend stores status/category/priority as free text (defaults: OPEN / PLUMBING / MEDIUM).
 * These unions are the frontend's canonical values to keep data consistent.
 */
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TicketCategory =
  | 'PLUMBING'
  | 'ELECTRICAL'
  | 'APPLIANCE'
  | 'CARPENTRY'
  | 'CLEANING'
  | 'PEST_CONTROL'
  | 'INTERNET'
  | 'OTHER';

export const TICKET_STATUSES: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
export const TICKET_PRIORITIES: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
export const TICKET_CATEGORIES: TicketCategory[] = [
  'PLUMBING',
  'ELECTRICAL',
  'APPLIANCE',
  'CARPENTRY',
  'CLEANING',
  'PEST_CONTROL',
  'INTERNET',
  'OTHER',
];

export interface MaintenanceTicket {
  id: string;
  organizationId: string;
  unitId: string;
  unitNumber: string;
  tenantId: string;
  tenantName: string;
  assignedToId?: string;
  title: string;
  description?: string;
  /** Typed as string since legacy rows may hold values outside TicketCategory. */
  category: TicketCategory | string;
  priority: TicketPriority | string;
  status: TicketStatus | string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TicketCreatePayload {
  unitId: string;
  tenantId: string;
  title: string;
  description?: string;
  category?: TicketCategory;
  priority?: TicketPriority;
}

export const ticketApi = {
  createTicket: async (payload: TicketCreatePayload): Promise<MaintenanceTicket> => {
    const response = await apiClient.post('/api/v1/tickets', payload);
    return response.data.data;
  },

  getTicketsByOrganization: async (): Promise<MaintenanceTicket[]> => {
    const response = await apiClient.get('/api/v1/tickets');
    return response.data.data;
  },

  getTicketsByTenant: async (tenantId: string): Promise<MaintenanceTicket[]> => {
    const response = await apiClient.get('/api/v1/tickets/tenant', {
      params: { tenantId },
    });
    return response.data.data;
  },

  updateTicketStatus: async (
    id: string,
    status: TicketStatus
  ): Promise<MaintenanceTicket> => {
    const response = await apiClient.patch(`/api/v1/tickets/${id}/status`, null, { params: { status } });
    return response.data.data;
  },
};
