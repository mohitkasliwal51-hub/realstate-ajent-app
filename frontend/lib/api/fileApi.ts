import { apiClient, API_BASE_URL } from './client';

/**
 * The server decides visibility from the category, the client never chooses it.
 * PROPERTY_IMAGE, ORG_LOGO  -> public (served from /uploads/public/**)
 * SIGNATURE, TENANT_KYC     -> private (needs login, same organization only)
 */
export type FileCategory = 'PROPERTY_IMAGE' | 'ORG_LOGO' | 'SIGNATURE' | 'TENANT_KYC';

export interface FileUploadResponse {
  /** Store this in the entity (imageUrl, agencyLogoUrl, idProofDocumentUrl, ...). */
  url: string;
  key: string;
  fileName: string;
  fileType: string;
  size: number;
  isPublic: boolean;
}

export const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

export const ACCEPT_BY_CATEGORY: Record<FileCategory, string> = {
  PROPERTY_IMAGE: 'image/jpeg,image/png,image/webp',
  ORG_LOGO: 'image/jpeg,image/png,image/webp',
  SIGNATURE: 'image/jpeg,image/png,image/webp',
  TENANT_KYC: 'image/jpeg,image/png,image/webp,application/pdf',
};

export const isPrivateFileUrl = (url: string): boolean =>
  url.includes('/api/v1/files/private');

/** Public files are stored as relative paths; make them absolute against the API host. */
export const resolveFileUrl = (url: string): string => {
  if (!url) return '';
  if (/^https?:\/\//i.test(url)) return url;
  return `${API_BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`;
};

export const fileApi = {
  uploadFile: async (file: File, category: FileCategory): Promise<FileUploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('category', category);

    // Content-Type must be left to the browser so the multipart boundary is set.
    const response = await apiClient.post('/api/v1/files/upload', formData, {
      headers: { 'Content-Type': undefined },
    });
    return response.data.data;
  },

  /** Private files need the bearer token, so fetch as a blob and open it in a new tab. */
  openFile: async (url: string): Promise<void> => {
    if (!isPrivateFileUrl(url)) {
      window.open(resolveFileUrl(url), '_blank', 'noopener,noreferrer');
      return;
    }
    const response = await apiClient.get(url, { responseType: 'blob' });
    const objectUrl = window.URL.createObjectURL(response.data as Blob);
    window.open(objectUrl, '_blank', 'noopener,noreferrer');
    // Give the new tab time to load before releasing the blob.
    setTimeout(() => window.URL.revokeObjectURL(objectUrl), 60_000);
  },
};
