import { apiClient } from './client';

export type PropertyType = 'PG' | 'HOSTEL' | 'FLAT' | 'COMMERCIAL';

export interface Property {
  id: string;
  organizationId: string;
  ownerId: string;
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
  organizationId: string;
  ownerId: string;
  name: string;
  type: PropertyType;
  address: string;
  city: string;
  state: string;
  pincode: string;
  landmark?: string;
  amenities?: string;
  description?: string;
}

export const propertyApi = {
  getProperties: async (organizationId: string): Promise<Property[]> => {
    const response = await apiClient.get('/api/v1/properties', {
      params: { organizationId },
    });
    return response.data.data;
  },

  getPropertyById: async (id: string, organizationId: string): Promise<Property> => {
    const response = await apiClient.get(`/api/v1/properties/${id}`, {
      params: { organizationId },
    });
    return response.data.data;
  },

  createProperty: async (payload: PropertyCreatePayload): Promise<Property> => {
    const response = await apiClient.post('/api/v1/properties', payload);
    return response.data.data;
  },
};
