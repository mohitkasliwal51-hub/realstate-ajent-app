import { apiClient } from './client';

export interface PanVerificationResult {
  valid: boolean;
  pan: string;
  registeredName?: string;
  type?: string;
  nameMatchScore?: string;
  message?: string;
}

export interface AadhaarMaskingResult {
  maskedImageUrl?: string;
  maskedDocumentUrl?: string;
  maskedAadhaarNumber?: string;
  name?: string;
  dob?: string;
  gender?: string;
  message?: string;
}

export interface DigiLockerInitiateResult {
  verificationId: string;
  url: string;
  status: string;
  message?: string;
}

export interface DigiLockerStatusResult {
  verificationId: string;
  status: string; // PENDING, COMPLETED, FAILED
  message?: string;
}

export interface DigiLockerCompleteResult {
  verificationId: string;
  status: string;
  aadhaarLast4?: string;
  fullName?: string;
  dob?: string;
  gender?: string;
  address?: string;
  message?: string;
}

export const kycApi = {
  verifyPan: async (pan: string, name?: string): Promise<PanVerificationResult> => {
    const response = await apiClient.post('/api/v1/kyc/pan/verify', {
      pan: pan.trim().toUpperCase(),
      name: name?.trim() || undefined,
    });
    return response.data.data;
  },

  maskAadhaar: async (file: File, tenantId?: string): Promise<AadhaarMaskingResult> => {
    const formData = new FormData();
    formData.append('file', file);
    if (tenantId) {
      formData.append('tenantId', tenantId);
    }
    const response = await apiClient.post('/api/v1/kyc/aadhaar/mask', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.data;
  },

  initiateDigiLocker: async (redirectUrl?: string): Promise<DigiLockerInitiateResult> => {
    const response = await apiClient.post('/api/v1/kyc/digilocker/initiate', {
      redirectUrl,
    });
    return response.data.data;
  },

  getDigiLockerStatus: async (
    verificationId: string,
    tenantId?: string
  ): Promise<DigiLockerStatusResult> => {
    const response = await apiClient.get(`/api/v1/kyc/digilocker/status/${verificationId}`, {
      params: tenantId ? { tenantId } : {},
    });
    return response.data.data;
  },

  completeDigiLocker: async (
    verificationId: string,
    tenantId?: string
  ): Promise<DigiLockerCompleteResult> => {
    const response = await apiClient.post(
      '/api/v1/kyc/digilocker/complete',
      null,
      {
        params: {
          verificationId,
          ...(tenantId ? { tenantId } : {}),
        },
      }
    );
    return response.data.data;
  },
};
