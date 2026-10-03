import { apiClient } from './client';
import { Property } from './propertyApi';
import type { OrganizationType } from '../auth/authTypes';

/** Full branding settings returned by GET /organizations/{id}/branding and saved by PUT */
export interface FullBrandingSettings {
  id?: string;
  legalBusinessName?: string;
  tradeName?: string;
  ownerPan?: string;
  ownerGstin?: string;
  reraNumber?: string;
  registeredOfficeAddress?: string;
  contactPhone?: string;
  contactEmail?: string;
  agencyLogoUrl?: string;
  primaryColor?: string;
  secondaryColor?: string;
  signatureUrl?: string;
  ownerUpiId?: string;
  bankAccountNumber?: string;
  bankIfscCode?: string;
  bankName?: string;
  accountHolderName?: string;
}

export interface BrandingResponse {
  legalBusinessName?: string;
  tradeName?: string;
  ownerGstin?: string;
  contactPhone?: string;
  contactEmail?: string;
  agencyLogoUrl?: string;
  primaryColor?: string;
  secondaryColor?: string;
}

export type BrandingUpdatePayload = FullBrandingSettings;
export type BrandingSettings = FullBrandingSettings;

export interface OrganizationDetails {
  id: string;
  name: string;
  slug: string;
  isActive: boolean;
  type?: OrganizationType;
  branding?: BrandingResponse;
}

export interface ShowcaseData {
  organizationName: string;
  organizationSlug: string;
  branding?: BrandingResponse;
  properties: Property[];
}

export const orgApi = {
  getOrganizationDetails: async (organizationId: string): Promise<OrganizationDetails> => {
    const response = await apiClient.get(`/api/v1/organizations/${organizationId}`);
    return response.data.data;
  },

  updateOrgType: async (organizationId: string, type: OrganizationType): Promise<OrganizationDetails> => {
    const response = await apiClient.patch(`/api/v1/organizations/${organizationId}/type`, { type });
    return response.data.data;
  },

  /** ADMIN / SUPER_ADMIN only. Gets full branding settings including bank details. */
  getFullBranding: async (organizationId: string): Promise<FullBrandingSettings> => {
    const response = await apiClient.get(`/api/v1/organizations/${organizationId}/branding`);
    return response.data.data;
  },

  /** ADMIN / SUPER_ADMIN only. Only non-null fields are applied. */
  updateBranding: async (organizationId: string, branding: FullBrandingSettings): Promise<FullBrandingSettings> => {
    const response = await apiClient.put(
      `/api/v1/organizations/${organizationId}/branding`,
      branding
    );
    return response.data.data;
  },

  /** Public (no auth). */
  getPublicShowcase: async (organizationSlug: string): Promise<ShowcaseData> => {
    const response = await apiClient.get(`/api/v1/showcase/${organizationSlug}`);
    return response.data.data;
  },
};
