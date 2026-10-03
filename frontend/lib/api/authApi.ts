import { apiClient } from './client';
import type { OrganizationType, UserProfile, UserRole } from '../auth/authTypes';

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: UserProfile;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  organizationName: string;
  organizationSlug?: string;
  /** Ignored by backend until it supports organizationType on register. */
  organizationType?: OrganizationType;
  fullName: string;
  email: string;
  password: string;
  phone?: string;
}

/** Raw shape of GET /api/v1/auth/me (uses profileId instead of id). */
interface MeResponse {
  profileId: string;
  email: string;
  fullName: string;
  phone?: string;
  avatarUrl?: string;
  role: UserRole;
  organizationId: string;
  organizationName?: string;
  organizationSlug?: string;
  organizationType?: OrganizationType;
  isActive?: boolean;
}

export const authApi = {
  login: async (payload: LoginPayload): Promise<AuthResponse> => {
    const response = await apiClient.post('/api/v1/auth/login', payload);
    return response.data.data;
  },

  register: async (payload: RegisterPayload): Promise<AuthResponse> => {
    const response = await apiClient.post('/api/v1/auth/register', payload);
    return response.data.data;
  },

  /** Returns the current user normalized to the UserProfile shape. */
  me: async (): Promise<UserProfile> => {
    const response = await apiClient.get('/api/v1/auth/me');
    const { profileId, ...rest }: MeResponse = response.data.data;
    return { id: profileId, ...rest };
  },
};
