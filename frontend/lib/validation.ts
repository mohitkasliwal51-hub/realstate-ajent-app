/**
 * Indian identity and financial format helpers.
 *
 * Rules mirror the backend (IndianFormats.java). Always run values through the
 * matching normalize* function before validating AND before sending to the API,
 * so what is validated is exactly what is stored.
 */

export const PAN_REGEX = /^[A-Z]{5}[0-9]{4}[A-Z]$/;
export const GSTIN_REGEX = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/;
export const IFSC_REGEX = /^[A-Z]{4}0[A-Z0-9]{6}$/;
export const PHONE_INDIA_REGEX = /^[6-9]\d{9}$/;
export const AADHAAR_REGEX = /^[2-9]\d{11}$/;
export const PINCODE_INDIA_REGEX = /^[1-9][0-9]{5}$/;
export const BANK_ACCOUNT_REGEX = /^\d{9,18}$/;
export const UPI_ID_REGEX = /^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64}$/;

export const normalizePan = (v: string) => v.replace(/\s/g, '').toUpperCase();
export const normalizeGstin = (v: string) => v.replace(/\s/g, '').toUpperCase();
export const normalizeIfsc = (v: string) => v.replace(/\s/g, '').toUpperCase();
export const normalizeAadhaar = (v: string) => v.replace(/[\s-]/g, '');
export const normalizePincode = (v: string) => v.replace(/\s/g, '');
export const normalizeBankAccount = (v: string) => v.replace(/[\s-]/g, '');
export const normalizeUpi = (v: string) => v.trim();

/** Strips spaces, dashes and a +91 / 91 / 0 prefix, leaving 10 digits when the input is a valid number. */
export const normalizeIndianPhone = (v: string): string => {
  const digits = v.replace(/[^\d]/g, '');
  if (digits.length === 12 && digits.startsWith('91')) return digits.slice(2);
  if (digits.length === 11 && digits.startsWith('0')) return digits.slice(1);
  return digits;
};

export const validatePan = (v: string) => PAN_REGEX.test(normalizePan(v));
export const validateGstin = (v: string) => GSTIN_REGEX.test(normalizeGstin(v));
export const validateIfsc = (v: string) => IFSC_REGEX.test(normalizeIfsc(v));
export const validateIndianPhone = (v: string) => PHONE_INDIA_REGEX.test(normalizeIndianPhone(v));
export const validateAadhaar = (v: string) => AADHAAR_REGEX.test(normalizeAadhaar(v));
export const validatePincode = (v: string) => PINCODE_INDIA_REGEX.test(normalizePincode(v));
export const validateBankAccount = (v: string) => BANK_ACCOUNT_REGEX.test(normalizeBankAccount(v));
export const validateUpi = (v: string) => UPI_ID_REGEX.test(normalizeUpi(v));

export const FORMAT_MESSAGES = {
  pan: 'Enter a valid PAN, e.g. ABCDE1234F',
  gstin: 'Enter a valid 15-character GSTIN, e.g. 29ABCDE1234F1Z5',
  ifsc: 'Enter a valid IFSC code, e.g. SBIN0001234',
  phone: 'Enter a valid 10-digit Indian mobile number',
  aadhaar: 'Enter a valid 12-digit Aadhaar number',
  pincode: 'Enter a valid 6-digit PIN code',
  bankAccount: 'Bank account number must be 9 to 18 digits',
  upi: 'Enter a valid UPI ID, e.g. name@bank',
} as const;

type Rule = { validate: (v: string) => boolean; message: string };

const RULES: Record<keyof typeof FORMAT_MESSAGES, Rule> = {
  pan: { validate: validatePan, message: FORMAT_MESSAGES.pan },
  gstin: { validate: validateGstin, message: FORMAT_MESSAGES.gstin },
  ifsc: { validate: validateIfsc, message: FORMAT_MESSAGES.ifsc },
  phone: { validate: validateIndianPhone, message: FORMAT_MESSAGES.phone },
  aadhaar: { validate: validateAadhaar, message: FORMAT_MESSAGES.aadhaar },
  pincode: { validate: validatePincode, message: FORMAT_MESSAGES.pincode },
  bankAccount: { validate: validateBankAccount, message: FORMAT_MESSAGES.bankAccount },
  upi: { validate: validateUpi, message: FORMAT_MESSAGES.upi },
};

/**
 * Returns an error message for a non-empty value that fails the rule, or null when it is
 * empty (optional field) or valid. Use `required` checks separately.
 */
export const formatError = (kind: keyof typeof FORMAT_MESSAGES, value?: string | null): string | null => {
  if (!value || !value.trim()) return null;
  const rule = RULES[kind];
  return rule.validate(value) ? null : rule.message;
};
