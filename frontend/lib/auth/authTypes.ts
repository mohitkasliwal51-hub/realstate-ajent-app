export type UserRole =
  | 'SUPER_ADMIN'
  | 'OWNER_ADMIN'
  | 'PROPERTY_MANAGER'
  | 'STAFF_ASSISTANT'
  | 'TENANT';

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  organizationId: string;
  organizationName?: string;
  tenantId?: string;
  phone?: string;
}

export interface AuthState {
  user: UserProfile | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
