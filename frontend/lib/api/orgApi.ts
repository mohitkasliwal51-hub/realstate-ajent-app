import { apiClient } from './client';
import { Property } from './propertyApi';

export interface BrandingSettings {
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

export interface OrganizationDetails {
  id: string;
  name: string;
  slug: string;
  isActive: boolean;
  branding?: BrandingSettings;
}

export interface ShowcaseData {
  organizationName: string;
  organizationSlug: string;
  branding?: BrandingSettings;
  properties: Property[];
}

export const orgApi = {
  getOrganizationDetails: async (organizationId: string): Promise<OrganizationDetails> => {
    const response = await apiClient.get(`/api/v1/organizations/${organizationId}`);
    return response.data.data;
  },

  updateBranding: async (
    organizationId: string,
    branding: BrandingSettings
  ): Promise<BrandingSettings> => {
    const response = await apiClient.put(
      `/api/v1/organizations/${organizationId}/branding`,
      branding
    );
    return response.data.data;
  },

  getPublicShowcase: async (organizationSlug: string): Promise<ShowcaseData> => {
    const response = await apiClient.get(`/api/v1/showcase/${organizationSlug}`);
    return response.data.data;
  },
};
