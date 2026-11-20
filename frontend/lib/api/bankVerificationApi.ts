import { apiClient } from './client';

export interface BankRpdInitiateResult {
  verificationId: string;
  refId?: string;
  status: string;
  message?: string;
  upiIntentUrl?: string;
  qrCode?: string;
  pspLinks?: Record<string, string>;
  validUpto?: string;
  url?: string;
}

export interface BankIfscLookupResult {
  valid: boolean;
  ifsc: string;
  bank?: string;
  branch?: string;
  address?: string;
  city?: string;
  state?: string;
  micr?: string;
  imps?: string;
  rtgs?: string;
  neft?: string;
  upi?: string;
  message?: string;
}

export interface BankVerificationResult {
  valid: boolean;
  verificationMode: 'REVERSE_PENNY_DROP' | 'BAV_SYNC' | 'UPI_PENNY_DROP';
  accountNumber?: string;
  ifsc?: string;
  bankName?: string;
  branch?: string;
  accountHolderName?: string;
  upiId?: string;
  nameMatchScore?: string;
  nameMatchResult?: string;
  utr?: string;
  referenceId?: string;
  message?: string;
  linkedToLandlord?: boolean;
  metadata?: Record<string, any>;
}

export const bankVerificationApi = {
  /**
   * Generates a dynamic ₹1 QR Code and UPI Intent link for Reverse Penny Drop (RPD).
   */
  initiateRpd: async (params?: {
    phone?: string;
    name?: string;
    email?: string;
    redirectUrl?: string;
    landlordId?: string;
  }): Promise<BankRpdInitiateResult> => {
    const response = await apiClient.post('/api/v1/bank-verification/rpd/initiate', params || {});
    return response.data.data;
  },

  /**
   * Polls the live status of an initiated Reverse Penny Drop verification.
   */
  checkRpdStatus: async (verificationId: string, landlordId?: string): Promise<BankVerificationResult> => {
    const response = await apiClient.get(`/api/v1/bank-verification/rpd/status/${verificationId}`, {
      params: landlordId ? { landlordId } : undefined,
    });
    return response.data.data;
  },

  /**
   * Fast real-time IFSC code validation and branch lookup.
   */
  lookupIfsc: async (ifsc: string): Promise<BankIfscLookupResult> => {
    const response = await apiClient.post('/api/v1/bank-verification/ifsc/lookup', {
      ifsc: ifsc.trim().toUpperCase(),
    });
    return response.data.data;
  },

  /**
   * Executes BAV V2 Sync Penny Drop to verify Account Number + IFSC and calculate Name Match Score.
   */
  verifyBankAccountSync: async (params: {
    bankAccount: string;
    ifsc: string;
    accountHolderName?: string;
    phone?: string;
    landlordId?: string;
  }): Promise<BankVerificationResult> => {
    const response = await apiClient.post('/api/v1/bank-verification/sync/verify', {
      bankAccount: params.bankAccount.trim(),
      ifsc: params.ifsc.trim().toUpperCase(),
      accountHolderName: params.accountHolderName?.trim() || undefined,
      phone: params.phone?.trim() || undefined,
      landlordId: params.landlordId || undefined,
    });
    return response.data.data;
  },

  /**
   * Executes UPI Penny Drop to verify VPA / UPI ID against NPCI.
   */
  verifyUpi: async (params: {
    vpa: string;
    name?: string;
    landlordId?: string;
  }): Promise<BankVerificationResult> => {
    const response = await apiClient.post('/api/v1/bank-verification/upi/verify', {
      vpa: params.vpa.trim().toLowerCase(),
      name: params.name?.trim() || undefined,
      landlordId: params.landlordId || undefined,
    });
    return response.data.data;
  },
};
