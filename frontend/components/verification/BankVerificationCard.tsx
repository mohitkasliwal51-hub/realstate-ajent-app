'use client';

import React, { useState, useEffect, useRef } from 'react';
import {
  CreditCard,
  QrCode,
  Smartphone,
  CheckCircle2,
  AlertTriangle,
  Building2,
  Sparkles,
  ArrowRight,
  RefreshCw,
  ExternalLink,
  ShieldCheck,
  Check,
  Copy,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import {
  bankVerificationApi,
  BankVerificationResult,
  BankRpdInitiateResult,
  BankIfscLookupResult,
} from '@/lib/api/bankVerificationApi';
import { useToast } from '@/lib/toast/ToastContext';
import { getErrorMessage } from '@/lib/utils';
import { validateIfsc, validateBankAccount } from '@/lib/validation';

interface BankVerificationCardProps {
  initialAccountHolderName?: string;
  landlordId?: string;
  onVerified?: (result: BankVerificationResult) => void;
  currentData?: {
    accountNumber?: string;
    ifsc?: string;
    bankName?: string;
    accountHolderName?: string;
    upiId?: string;
    isVerified?: boolean;
  };
}

export const BankVerificationCard: React.FC<BankVerificationCardProps> = ({
  initialAccountHolderName = '',
  landlordId,
  onVerified,
  currentData,
}) => {
  const { toast } = useToast();

  // Primary verification state
  const [isVerified, setIsVerified] = useState(currentData?.isVerified || false);
  const [verifiedResult, setVerifiedResult] = useState<BankVerificationResult | null>(
    currentData?.isVerified
      ? {
          valid: true,
          verificationMode: currentData.upiId ? 'UPI_PENNY_DROP' : 'BAV_SYNC',
          accountNumber: currentData.accountNumber,
          ifsc: currentData.ifsc,
          bankName: currentData.bankName,
          accountHolderName: currentData.accountHolderName,
          upiId: currentData.upiId,
        }
      : null
  );

  // Flow mode: RPD (default automatic) vs MANUAL
  const [mode, setMode] = useState<'RPD' | 'MANUAL'>('RPD');
  const [manualTab, setManualTab] = useState<'BANK' | 'UPI'>('BANK');

  // Reverse Penny Drop State
  const [rpdData, setRpdData] = useState<BankRpdInitiateResult | null>(null);
  const [isGeneratingRpd, setIsGeneratingRpd] = useState(false);
  const [isPollingRpd, setIsPollingRpd] = useState(false);
  const [countdown, setCountdown] = useState(600); // 10 minutes
  const pollTimerRef = useRef<NodeJS.Timeout | null>(null);

  // Manual Bank Tab State
  const [accountHolderName, setAccountHolderName] = useState(
    currentData?.accountHolderName || initialAccountHolderName
  );
  const [bankAccount, setBankAccount] = useState(currentData?.accountNumber || '');
  const [ifsc, setIfsc] = useState(currentData?.ifsc || '');
  const [isLookingUpIfsc, setIsLookingUpIfsc] = useState(false);
  const [ifscDetails, setIfscDetails] = useState<BankIfscLookupResult | null>(null);
  const [isVerifyingBank, setIsVerifyingBank] = useState(false);

  // Manual UPI Tab State
  const [vpa, setVpa] = useState(currentData?.upiId || '');
  const [isVerifyingUpi, setIsVerifyingUpi] = useState(false);

  // Update accountHolderName if prop changes and field wasn't edited
  useEffect(() => {
    if (initialAccountHolderName && !accountHolderName) {
      setAccountHolderName(initialAccountHolderName);
    }
  }, [initialAccountHolderName]);

  // Clean up polling timer on unmount
  useEffect(() => {
    return () => {
      if (pollTimerRef.current) clearInterval(pollTimerRef.current);
    };
  }, []);

  // Countdown timer for RPD QR code
  useEffect(() => {
    if (!rpdData || isVerified) return;
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          if (pollTimerRef.current) clearInterval(pollTimerRef.current);
          setIsPollingRpd(false);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [rpdData, isVerified]);

  // Auto-lookup IFSC as soon as 11 valid characters are typed
  useEffect(() => {
    const cleanIfsc = ifsc.trim().toUpperCase();
    if (cleanIfsc.length === 11 && validateIfsc(cleanIfsc)) {
      triggerIfscLookup(cleanIfsc);
    } else {
      setIfscDetails(null);
    }
  }, [ifsc]);

  // 1. Trigger Reverse Penny Drop (RPD)
  const handleInitiateRpd = async () => {
    setIsGeneratingRpd(true);
    setRpdData(null);
    try {
      const data = await bankVerificationApi.initiateRpd({
        name: accountHolderName || undefined,
        landlordId,
      });
      setRpdData(data);
      setCountdown(600);
      setIsPollingRpd(true);
      toast.info('QR Code generated! Scan with GPay, PhonePe, or Paytm to verify.');

      // Start polling every 3.5 seconds
      if (pollTimerRef.current) clearInterval(pollTimerRef.current);
      pollTimerRef.current = setInterval(async () => {
        try {
          const statusResult = await bankVerificationApi.checkRpdStatus(data.verificationId, landlordId);
          if (statusResult.valid) {
            if (pollTimerRef.current) clearInterval(pollTimerRef.current);
            setIsPollingRpd(false);
            setIsVerified(true);
            setVerifiedResult(statusResult);
            toast.success(`Bank Account Verified: ${statusResult.accountHolderName || 'Success'}`);
            if (onVerified) onVerified(statusResult);
          }
        } catch {
          // Continue polling on transient errors
        }
      }, 3500);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to generate Reverse Penny Drop QR.'));
    } finally {
      setIsGeneratingRpd(false);
    }
  };

  // 2. IFSC Lookup
  const triggerIfscLookup = async (ifscCode: string) => {
    setIsLookingUpIfsc(true);
    try {
      const data = await bankVerificationApi.lookupIfsc(ifscCode);
      if (data.valid) {
        setIfscDetails(data);
      } else {
        setIfscDetails(null);
      }
    } catch {
      setIfscDetails(null);
    } finally {
      setIsLookingUpIfsc(false);
    }
  };

  // 3. Manual Bank Account Penny Drop (BAV V2 Sync)
  const handleVerifyBankSync = async () => {
    if (!bankAccount || !validateBankAccount(bankAccount)) {
      toast.warning('Please enter a valid bank account number (9 to 18 digits).');
      return;
    }
    if (!ifsc || !validateIfsc(ifsc)) {
      toast.warning('Please enter a valid 11-character IFSC code (e.g. HDFC0001234).');
      return;
    }

    setIsVerifyingBank(true);
    try {
      const result = await bankVerificationApi.verifyBankAccountSync({
        bankAccount,
        ifsc,
        accountHolderName,
        landlordId,
      });

      if (result.valid) {
        setIsVerified(true);
        setVerifiedResult(result);
        toast.success(
          `Bank Account Verified! Registered Name: ${result.accountHolderName || 'Active Account'}`
        );
        if (onVerified) onVerified(result);
      } else {
        toast.error(result.message || 'Bank account verification failed. Please check details.');
      }
    } catch (err) {
      toast.error(getErrorMessage(err, 'Cashfree Penny Drop verification failed.'));
    } finally {
      setIsVerifyingBank(false);
    }
  };

  // 4. Manual UPI Penny Drop
  const handleVerifyUpi = async () => {
    if (!vpa || !vpa.includes('@')) {
      toast.warning('Please enter a valid UPI ID (e.g. username@okhdfcbank).');
      return;
    }

    setIsVerifyingUpi(true);
    try {
      const result = await bankVerificationApi.verifyUpi({
        vpa,
        name: accountHolderName,
        landlordId,
      });

      if (result.valid) {
        setIsVerified(true);
        setVerifiedResult(result);
        toast.success(`UPI Verified: ${result.accountHolderName || vpa}`);
        if (onVerified) onVerified(result);
      } else {
        toast.error(result.message || 'UPI ID verification failed.');
      }
    } catch (err) {
      toast.error(getErrorMessage(err, 'UPI Penny Drop verification failed.'));
    } finally {
      setIsVerifyingUpi(false);
    }
  };

  // Reset verification to allow editing
  const handleResetVerification = () => {
    if (pollTimerRef.current) clearInterval(pollTimerRef.current);
    setIsVerified(false);
    setVerifiedResult(null);
    setRpdData(null);
    setIsPollingRpd(false);
  };

  // Format countdown minutes:seconds
  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  return (
    <div className="space-y-4">
      {/* VERIFIED SUMMARY CARD (Shown when account is verified) */}
      {isVerified && verifiedResult ? (
        <div className="p-6 rounded-3xl bg-linear-to-br from-emerald-50 via-teal-50 to-green-50 border border-emerald-200/80 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-2xl bg-emerald-600 flex items-center justify-center text-white shadow-xs">
                <CheckCircle2 className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-bold text-emerald-950 flex items-center gap-2">
                  Payout Bank Account Verified
                  <Badge variant="success" dot className="bg-emerald-100 text-emerald-800 border-emerald-300">
                    {verifiedResult.verificationMode === 'REVERSE_PENNY_DROP'
                      ? 'AUTO-VERIFIED (UPI RPD)'
                      : verifiedResult.verificationMode === 'UPI_PENNY_DROP'
                      ? 'UPI VERIFIED'
                      : 'BAV V2 PENNY DROP VERIFIED'}
                  </Badge>
                </h4>
                <p className="text-xs text-emerald-700">
                  Confirmed active via Cashfree Secure ID & NPCI banking network.
                </p>
              </div>
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleResetVerification}
              className="text-xs border-emerald-300 text-emerald-800 hover:bg-emerald-100/50"
            >
              Change Account
            </Button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 pt-1">
            <div className="p-3 bg-white/80 rounded-2xl border border-emerald-100">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 block">
                Account Holder Name
              </span>
              <span className="text-xs font-bold text-slate-900 truncate block mt-0.5">
                {verifiedResult.accountHolderName || 'Verified Beneficiary'}
              </span>
            </div>

            {verifiedResult.accountNumber && (
              <div className="p-3 bg-white/80 rounded-2xl border border-emerald-100">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 block">
                  Bank Account Number
                </span>
                <span className="text-xs font-mono font-bold text-slate-900 block mt-0.5">
                  {verifiedResult.accountNumber}
                </span>
              </div>
            )}

            {verifiedResult.ifsc && (
              <div className="p-3 bg-white/80 rounded-2xl border border-emerald-100">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 block">
                  IFSC & Bank
                </span>
                <span className="text-xs font-mono font-bold text-slate-900 block mt-0.5">
                  {verifiedResult.ifsc}
                </span>
                {verifiedResult.bankName && (
                  <span className="text-[10px] text-slate-600 block truncate">
                    {verifiedResult.bankName}
                  </span>
                )}
              </div>
            )}

            {verifiedResult.upiId && (
              <div className="p-3 bg-white/80 rounded-2xl border border-emerald-100">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 block">
                  Payout UPI ID
                </span>
                <span className="text-xs font-mono font-bold text-slate-900 block mt-0.5">
                  {verifiedResult.upiId}
                </span>
              </div>
            )}

            {verifiedResult.utr && (
              <div className="p-3 bg-white/80 rounded-2xl border border-emerald-100">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 block">
                  Bank Transfer UTR
                </span>
                <span className="text-xs font-mono text-slate-700 block mt-0.5">
                  {verifiedResult.utr}
                </span>
              </div>
            )}

            {verifiedResult.nameMatchScore && (
              <div className="p-3 bg-white/80 rounded-2xl border border-emerald-100">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 block">
                  Name Match Score
                </span>
                <span className="text-xs font-bold text-emerald-700 block mt-0.5">
                  {verifiedResult.nameMatchScore}% ({verifiedResult.nameMatchResult?.replace('_', ' ') || 'Match'})
                </span>
              </div>
            )}
          </div>
        </div>
      ) : (
        /* UNVERIFIED STATE: Default RPD + Fallback Manual */
        <div className="space-y-4">
          {mode === 'RPD' ? (
            /* 1. REVERSE PENNY DROP VIEW (AUTOMATIC QR/INTENT) */
            <div className="p-6 rounded-3xl bg-linear-to-b from-blue-50/60 to-white border border-blue-200/80 shadow-xs space-y-5">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-2xl bg-blue-600 text-white flex items-center justify-center shadow-md shadow-blue-500/20">
                    <QrCode className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                      Instant Auto-Verification via UPI
                      <Badge variant="info" className="text-[10px] uppercase font-bold tracking-wider">
                        Recommended
                      </Badge>
                    </h4>
                    <p className="text-xs text-slate-600">
                      Scan & approve ₹1 using any UPI app (GPay, PhonePe, Paytm). Bank account details and legal name are auto-verified without typing. ₹1 is auto-refunded.
                    </p>
                  </div>
                </div>

                {!rpdData && (
                  <Button
                    type="button"
                    onClick={handleInitiateRpd}
                    isLoading={isGeneratingRpd}
                    className="font-bold text-xs shrink-0 shadow-md shadow-blue-500/15"
                  >
                    <Sparkles className="w-3.5 h-3.5 mr-1" /> Generate ₹1 QR Code
                  </Button>
                )}
              </div>

              {/* RPD ACTIVE CARD WITH QR AND INTENT */}
              {rpdData && (
                <div className="p-5 rounded-2xl bg-white border border-blue-200 shadow-sm space-y-4">
                  <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
                    {/* QR Code Container */}
                    <div className="flex flex-col items-center p-3 rounded-2xl bg-slate-50 border border-slate-200">
                      {rpdData.qrCode ? (
                        <img
                          src={rpdData.qrCode.startsWith('data:') ? rpdData.qrCode : `data:image/png;base64,${rpdData.qrCode}`}
                          alt="Cashfree UPI Reverse Penny Drop QR"
                          className="w-44 h-44 object-contain rounded-xl"
                        />
                      ) : (
                        <div className="w-44 h-44 flex items-center justify-center text-xs text-slate-400">
                          QR Loading...
                        </div>
                      )}
                      <div className="mt-2 text-center">
                        <span className="text-[11px] font-bold text-slate-700">Amount: ₹1.00</span>
                        <span className="text-[10px] text-slate-500 block">Expires in: {formatTime(countdown)}</span>
                      </div>
                    </div>

                    {/* Instructions & Mobile Intent Links */}
                    <div className="flex-1 space-y-3 text-left">
                      <div className="space-y-1">
                        <h5 className="text-xs font-bold text-slate-900 flex items-center gap-1.5">
                          {isPollingRpd ? (
                            <>
                              <RefreshCw className="w-3.5 h-3.5 text-blue-600 animate-spin" />
                              Listening for bank confirmation...
                            </>
                          ) : (
                            'Waiting for verification'
                          )}
                        </h5>
                        <p className="text-xs text-slate-600">
                          1. Open <strong>Google Pay</strong>, <strong>PhonePe</strong>, or <strong>Paytm</strong> on your phone.
                          <br />
                          2. Scan the dynamic QR code on the left and pay ₹1.
                          <br />
                          3. Your bank details will be confirmed automatically within 3 seconds!
                        </p>
                      </div>

                      {/* UPI Intent Links (for mobile screens) */}
                      {rpdData.pspLinks && (
                        <div className="pt-2">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 block mb-1.5">
                            Tap to pay directly via app:
                          </span>
                          <div className="flex flex-wrap gap-2">
                            {rpdData.pspLinks.gpay && (
                              <a
                                href={rpdData.pspLinks.gpay}
                                className="px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-xs font-bold text-slate-800 inline-flex items-center gap-1"
                              >
                                <Smartphone className="w-3.5 h-3.5 text-blue-600" /> Google Pay
                              </a>
                            )}
                            {rpdData.pspLinks.phonepe && (
                              <a
                                href={rpdData.pspLinks.phonepe}
                                className="px-3 py-1.5 rounded-xl bg-purple-50 hover:bg-purple-100 text-xs font-bold text-purple-800 inline-flex items-center gap-1"
                              >
                                <Smartphone className="w-3.5 h-3.5 text-purple-600" /> PhonePe
                              </a>
                            )}
                            {rpdData.pspLinks.paytm && (
                              <a
                                href={rpdData.pspLinks.paytm}
                                className="px-3 py-1.5 rounded-xl bg-sky-50 hover:bg-sky-100 text-xs font-bold text-sky-800 inline-flex items-center gap-1"
                              >
                                <Smartphone className="w-3.5 h-3.5 text-sky-600" /> Paytm
                              </a>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* Toggle to Manual Form */}
              <div className="flex items-center justify-between pt-2 border-t border-blue-100">
                <span className="text-xs text-slate-500">Don't have UPI or prefer entering bank details?</span>
                <button
                  type="button"
                  onClick={() => setMode('MANUAL')}
                  className="text-xs font-bold text-blue-600 hover:text-blue-800 transition-colors flex items-center gap-1"
                >
                  Enter details manually ➔
                </button>
              </div>
            </div>
          ) : (
            /* 2. MANUAL VERIFICATION VIEW (TABS: BANK ACCOUNT vs UPI) */
            <div className="p-6 rounded-3xl bg-white border border-slate-200 shadow-xs space-y-4">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                <div className="flex items-center gap-2">
                  <CreditCard className="w-4 h-4 text-slate-700" />
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-900">
                    Manual Bank Verification
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => setMode('RPD')}
                  className="text-xs font-bold text-blue-600 hover:text-blue-800 flex items-center gap-1"
                >
                  ⚡ Switch to Instant UPI QR
                </button>
              </div>

              {/* Tabs for Bank Details vs UPI ID */}
              <div className="flex gap-2 p-1 bg-slate-100 rounded-2xl w-fit">
                <button
                  type="button"
                  onClick={() => setManualTab('BANK')}
                  className={`px-4 py-1.5 rounded-xl text-xs font-bold transition-all ${
                    manualTab === 'BANK'
                      ? 'bg-white text-slate-900 shadow-xs'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Bank Account & IFSC
                </button>
                <button
                  type="button"
                  onClick={() => setManualTab('UPI')}
                  className={`px-4 py-1.5 rounded-xl text-xs font-bold transition-all ${
                    manualTab === 'UPI'
                      ? 'bg-white text-slate-900 shadow-xs'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  UPI ID (VPA)
                </button>
              </div>

              {manualTab === 'BANK' ? (
                /* TAB 1: BANK ACCOUNT & IFSC */
                <div className="space-y-4 pt-1">
                  <Input
                    label="Account Holder Name"
                    placeholder="e.g. Sunil Patel (Name as per Bank Passbook)"
                    value={accountHolderName}
                    onChange={(e) => setAccountHolderName(e.target.value)}
                    helperText="Pre-filled from KYC/GSTIN record. Used for Cashfree fuzzy name matching."
                  />

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 items-start">
                    <Input
                      label="Bank Account Number *"
                      placeholder="e.g. 026291800001191"
                      value={bankAccount}
                      onChange={(e) => setBankAccount(e.target.value.replace(/\s+/g, ''))}
                      required
                    />

                    <div>
                      <Input
                        label="IFSC Code *"
                        placeholder="e.g. HDFC0001234"
                        value={ifsc}
                        onChange={(e) => setIfsc(e.target.value.toUpperCase().replace(/\s+/g, ''))}
                        maxLength={11}
                        className="font-mono uppercase font-bold"
                        required
                        helperText={isLookingUpIfsc ? 'Validating IFSC branch...' : '11-character bank branch identifier'}
                      />
                    </div>
                  </div>

                  {/* REAL-TIME IFSC BRANCH PREVIEW CARD */}
                  {ifscDetails && (
                    <div className="p-3.5 rounded-2xl bg-blue-50/80 border border-blue-200/90 text-xs text-blue-950 flex items-start justify-between gap-3 animate-fade-in">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <Building2 className="w-4 h-4 text-blue-700 shrink-0" />
                          <span className="font-bold text-slate-900">{ifscDetails.bank || 'Verified Bank'}</span>
                          <Badge variant="info" className="text-[10px] font-mono">
                            {ifscDetails.ifsc}
                          </Badge>
                        </div>
                        <p className="text-[11px] text-slate-600">
                          Branch: <strong>{ifscDetails.branch || 'Branch'}</strong>, {ifscDetails.city}, {ifscDetails.state}
                        </p>
                        <p className="text-[10px] text-blue-700 font-medium">
                          Supported Rails: IMPS {ifscDetails.imps || 'Live'} • RTGS {ifscDetails.rtgs || 'Live'} • NEFT {ifscDetails.neft || 'Live'}
                        </p>
                      </div>
                      <Badge variant="success" dot className="shrink-0">
                        IFSC ACTIVE
                      </Badge>
                    </div>
                  )}

                  <div className="pt-2">
                    <Button
                      type="button"
                      onClick={handleVerifyBankSync}
                      isLoading={isVerifyingBank}
                      disabled={!bankAccount || !ifsc}
                      className="w-full sm:w-auto font-bold text-xs shadow-md shadow-blue-500/20"
                    >
                      Verify Account via Penny Drop (₹1) ➔
                    </Button>
                    <p className="text-[11px] text-slate-500 mt-1.5">
                      Cashfree will initiate an instant IMPS micro-deposit of ₹1 to verify your account status and registered legal name.
                    </p>
                  </div>
                </div>
              ) : (
                /* TAB 2: UPI ID (VPA) */
                <div className="space-y-4 pt-1">
                  <Input
                    label="Account Holder Name (Optional)"
                    placeholder="e.g. Sunil Patel"
                    value={accountHolderName}
                    onChange={(e) => setAccountHolderName(e.target.value)}
                  />

                  <Input
                    label="UPI ID / VPA *"
                    placeholder="e.g. owner@okhdfcbank or agency@icici"
                    value={vpa}
                    onChange={(e) => setVpa(e.target.value.toLowerCase().replace(/\s+/g, ''))}
                    helperText="Rent payouts and security deposits will be disbursed directly to this UPI address."
                    required
                  />

                  <div className="pt-2">
                    <Button
                      type="button"
                      onClick={handleVerifyUpi}
                      isLoading={isVerifyingUpi}
                      disabled={!vpa || !vpa.includes('@')}
                      className="w-full sm:w-auto font-bold text-xs shadow-md shadow-blue-500/20"
                    >
                      Verify UPI ID ➔
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
