export type UserRole =
  | 'SUPER_ADMIN'
  | 'ADMIN'
  | 'PROPERTY_MANAGER'
  | 'AGENT'
  | 'TENANT'
  | 'PUBLIC_GUEST';

/**
 * Business model of the organization.
 * - OWNER: owner/landlord managing their own properties (no landlords directory, no payouts)
 * - BROKERAGE: broker/agency managing properties on behalf of landlords
 * - HYBRID: both (owns some properties, manages others)
 *
 * NOTE: backend entity has this field but does not expose it yet in auth/profile
 * responses, so it is optional here. See lib/orgMode.ts for the fallback logic.
 */
export type OrganizationType = 'OWNER' | 'BROKERAGE' | 'HYBRID';

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  organizationId: string;
  organizationName?: string;
  organizationSlug?: string;
  organizationType?: OrganizationType;
  phone?: string;
  avatarUrl?: string;
  isActive?: boolean;
  /** Not provided by backend yet (no tenant self-lookup endpoint). */
  tenantId?: string;
}

export interface AuthState {
  user: UserProfile | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
