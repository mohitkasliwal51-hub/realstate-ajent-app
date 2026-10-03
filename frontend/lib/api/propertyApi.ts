import { apiClient } from './client';

export type PropertyType = 'PG' | 'HOSTEL' | 'FLAT' | 'COMMERCIAL';

export interface Property {
  id: string;
  organizationId: string;
  /** Set when the property is managed on behalf of a landlord (broker mode). Null for self-owned. */
  landlordId?: string;
  landlordName?: string;
  name: string;
  type: PropertyType;
  address: string;
  city: string;
  state: string;
  pincode: string;
  landmark?: string;
  latitude?: number;
  longitude?: number;
  amenities?: string;
  rules?: string[];
  images?: string[];
  description?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PropertyCreatePayload {
  /** Optional: omit for self-owned properties (owner mode). */
  landlordId?: string;
  name: string;
  type: PropertyType;
  address: string;
  city: string;
  state: string;
  pincode: string;
  landmark?: string;
  latitude?: number;
  longitude?: number;
  amenities?: string;
  rules?: string[];
  images?: string[];
  description?: string;
}

export const propertyApi = {
  getProperties: async (): Promise<Property[]> => {
    const response = await apiClient.get('/api/v1/properties');
    return response.data.data;
  },

  getPropertyById: async (id: string): Promise<Property> => {
    const response = await apiClient.get(`/api/v1/properties/${id}`);
    return response.data.data;
  },

  createProperty: async (payload: PropertyCreatePayload): Promise<Property> => {
    const response = await apiClient.post('/api/v1/properties', payload);
    return response.data.data;
  },

  updateProperty: async (id: string, payload: PropertyCreatePayload): Promise<Property> => {
    const response = await apiClient.put(`/api/v1/properties/${id}`, payload);
    return response.data.data;
  },

  togglePropertyActive: async (id: string, isActive: boolean): Promise<Property> => {
    const response = await apiClient.patch(`/api/v1/properties/${id}/active?isActive=${isActive}`);
    return response.data.data;
  },

  /** Public (no auth): all active properties. */
  getPublicProperties: async (): Promise<Property[]> => {
    const response = await apiClient.get('/api/v1/properties/public');
    return response.data.data;
  },
};
