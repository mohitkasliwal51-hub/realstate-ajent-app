import { apiClient } from './client';

export interface GstinVerificationResult {
  valid: boolean;
  gstin: string;
  legalName?: string;
  tradeName?: string;
  registrationDate?: string;
  constitution?: string;
  taxpayerType?: string;
  status?: string;
  centerJurisdiction?: string;
  stateJurisdiction?: string;
  lastUpdateDate?: string;
  address?: string;
  message?: string;
  referenceId?: string;
  linkedToLandlord?: boolean;
}

export interface GstinRecord {
  gstin: string;
  status: string;
  state: string;
}

export interface PanToGstinResult {
  pan: string;
  verificationId?: string;
  status?: string;
  gstins: GstinRecord[];
  referenceId?: string;
  linkedToLandlord?: boolean;
}

export interface CinVerificationResult {
  valid: boolean;
  cin: string;
  verificationId?: string;
  companyName?: string;
  registrationNumber?: string;
  incorporationDate?: string;
  companyStatus?: string;
  email?: string;
  incorporationCountry?: string;
  directors?: any[];
  referenceId?: string;
  linkedToLandlord?: boolean;
}

export const kybApi = {
  /**
   * Verify GSTIN against government GST database via Cashfree.
   * Returns official legal business name, trade name, constitution, and registered address.
   */
  verifyGstin: async (
    gstin: string,
    businessName?: string,
    landlordId?: string
  ): Promise<GstinVerificationResult> => {
    const response = await apiClient.post('/api/v1/kyb/gstin/verify', {
      gstin: gstin.trim().toUpperCase(),
      businessName: businessName?.trim() || undefined,
      landlordId: landlordId || undefined,
    });
    return response.data.data;
  },

  /**
   * Look up all registered GSTINs linked to a business/individual PAN across all Indian states.
   */
  findGstinsByPan: async (
    pan: string,
    landlordId?: string
  ): Promise<PanToGstinResult> => {
    const response = await apiClient.post('/api/v1/kyb/pan-to-gstin', {
      pan: pan.trim().toUpperCase(),
      landlordId: landlordId || undefined,
    });
    return response.data.data;
  },

  /**
   * Verify Corporate Identification Number (CIN) against Ministry of Corporate Affairs (MCA).
   */
  verifyCin: async (
    cin: string,
    landlordId?: string
  ): Promise<CinVerificationResult> => {
    const response = await apiClient.post('/api/v1/kyb/cin/verify', {
      cin: cin.trim().toUpperCase(),
      landlordId: landlordId || undefined,
    });
    return response.data.data;
  },
};
