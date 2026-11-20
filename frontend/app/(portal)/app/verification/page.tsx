'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  ShieldCheck,
  CreditCard,
  Building2,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  ArrowLeft,
  Sparkles,
  Search,
  FileText,
  MapPin,
  Clock,
  Briefcase,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { kycApi } from '@/lib/api/kycApi';
import { kybApi, GstinRecord, GstinVerificationResult, CinVerificationResult } from '@/lib/api/kybApi';
import { orgApi } from '@/lib/api/orgApi';
import { useAuth } from '@/lib/auth/AuthContext';
import { useToast } from '@/lib/toast/ToastContext';
import { getErrorMessage } from '@/lib/utils';
import { validatePan, validateGstin, validateIfsc } from '@/lib/validation';

import { BankVerificationCard } from '@/components/verification/BankVerificationCard';
import { BankVerificationResult } from '@/lib/api/bankVerificationApi';

export default function OwnerVerificationPage() {
  const router = useRouter();
  const { user } = useAuth();
  const { toast } = useToast();

  // PAN State
  const [pan, setPan] = useState('');
  const [name, setName] = useState('');
  const [isPanLoading, setIsPanLoading] = useState(false);
  const [isPanVerified, setIsPanVerified] = useState(false);
  const [panDetails, setPanDetails] = useState<{
    registeredName?: string;
    entityType?: string;
  } | null>(null);

  // PAN-to-GSTIN Auto-Discovery State
  const [isSearchingGstins, setIsSearchingGstins] = useState(false);
  const [discoveredGstins, setDiscoveredGstins] = useState<GstinRecord[]>([]);

  // GSTIN State
  const [gstin, setGstin] = useState('');
  const [isGstinLoading, setIsGstinLoading] = useState(false);
  const [isGstinVerified, setIsGstinVerified] = useState(false);
  const [gstinDetails, setGstinDetails] = useState<GstinVerificationResult | null>(null);

  // Optional CIN State
  const [showCinSection, setShowCinSection] = useState(false);
  const [cin, setCin] = useState('');
  const [isCinLoading, setIsCinLoading] = useState(false);
  const [isCinVerified, setIsCinVerified] = useState(false);
  const [cinDetails, setCinDetails] = useState<CinVerificationResult | null>(null);

  // Bank Details State
  const [bankAccount, setBankAccount] = useState('');
  const [ifsc, setIfsc] = useState('');
  const [bankName, setBankName] = useState('');
  const [isBankVerified, setIsBankVerified] = useState(false);

  // Submission State
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 1. Verify PAN & Auto-Discover GSTINs
  const handleVerifyPan = async () => {
    if (!pan || !validatePan(pan)) {
      toast.warning('Please enter a valid 10-character PAN number (e.g. ABCDE1234F).');
      return;
    }

    setIsPanLoading(true);
    try {
      const data = await kycApi.verifyPan(pan, name);
      if (data.valid) {
        setIsPanVerified(true);
        setPanDetails({
          registeredName: data.registeredName,
          entityType: data.type,
        });
        if (data.registeredName && !name) {
          setName(data.registeredName);
        }
        toast.success(`PAN Verified: ${data.registeredName || pan}`);

        // Trigger PAN-to-GSTIN Discovery
        discoverGstins(pan);
      } else {
        toast.error(`PAN Verification Failed: ${data.message || 'Invalid PAN'}`);
      }
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to verify PAN with Cashfree.'));
    } finally {
      setIsPanLoading(false);
    }
  };

  // 2. Discover GSTINs via PAN
  const discoverGstins = async (panNumber: string) => {
    setIsSearchingGstins(true);
    try {
      const res = await kybApi.findGstinsByPan(panNumber);
      if (res.gstins && res.gstins.length > 0) {
        setDiscoveredGstins(res.gstins);
        toast.info(`Found ${res.gstins.length} GSTIN(s) linked to PAN ${panNumber}!`);
      }
    } catch (err) {
      // Non-blocking discovery
      console.log('PAN-to-GSTIN lookup did not find records', err);
    } finally {
      setIsSearchingGstins(false);
    }
  };

  // 3. Verify GSTIN
  const handleVerifyGstin = async (gstinToVerify?: string) => {
    const targetGstin = (gstinToVerify || gstin).trim().toUpperCase();
    if (!targetGstin || !validateGstin(targetGstin)) {
      toast.warning('Please enter a valid 15-character GSTIN (e.g. 29ABCDE1234F1Z5).');
      return;
    }

    setIsGstinLoading(true);
    try {
      const data = await kybApi.verifyGstin(targetGstin);
      if (data.valid) {
        setIsGstinVerified(true);
        setGstinDetails(data);
        setGstin(targetGstin);
        if (data.legalName && !name) {
          setName(data.legalName);
        }
        toast.success(`GSTIN Verified: ${data.legalName || data.tradeName || targetGstin}`);
      } else {
        toast.error(`GSTIN Verification Failed: ${data.message || 'Invalid GSTIN'}`);
      }
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to verify GSTIN with Cashfree.'));
    } finally {
      setIsGstinLoading(false);
    }
  };

  // 4. Verify CIN
  const handleVerifyCin = async () => {
    const targetCin = cin.trim().toUpperCase();
    if (!targetCin || targetCin.length !== 21) {
      toast.warning('Please enter a valid 21-character MCA CIN (e.g. U72900KA2015PTC082988).');
      return;
    }

    setIsCinLoading(true);
    try {
      const data = await kybApi.verifyCin(targetCin);
      if (data.valid) {
        setIsCinVerified(true);
        setCinDetails(data);
        toast.success(`MCA CIN Verified: ${data.companyName}`);
      } else {
        toast.error('CIN Verification Failed: Company not found or inactive in MCA database.');
      }
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to verify CIN with MCA database.'));
    } finally {
      setIsCinLoading(false);
    }
  };

  // 5. Complete Setup & Save to Organization Branding
  const handleCompleteSetup = async () => {
    if (!isPanVerified) {
      toast.warning('Please verify your PAN card number first.');
      return;
    }
    if (!bankAccount.trim() || !ifsc.trim()) {
      toast.warning('Please enter your rent settlement bank account and IFSC code.');
      return;
    }
    if (!validateIfsc(ifsc)) {
      toast.warning('Please enter a valid 11-character IFSC Code (e.g. HDFC0001234).');
      return;
    }

    setIsSubmitting(true);
    try {
      if (user?.organizationId) {
        await orgApi.updateBranding(user.organizationId, {
          legalBusinessName: gstinDetails?.legalName || cinDetails?.companyName || name || 'Verified Business',
          tradeName: gstinDetails?.tradeName || name,
          ownerPan: pan,
          ownerGstin: isGstinVerified ? gstin : undefined,
          bankAccountNumber: bankAccount.trim(),
          bankIfscCode: ifsc.trim().toUpperCase(),
          bankName: bankName.trim() || undefined,
          registeredOfficeAddress: gstinDetails?.address || undefined,
        });
      }

      toast.success('Owner profile & KYB verified successfully! You can now list properties.');
      router.push('/app/properties/new');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to save verified organization details.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6 pb-12 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex items-center justify-between">
        <Link
          href="/app/properties"
          className="text-xs font-semibold text-slate-500 hover:text-slate-800 flex items-center gap-1"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Properties
        </Link>
        <Badge variant="purple">Landlord KYB Compliance</Badge>
      </div>

      <div className="space-y-1.5">
        <h1 className="text-2xl font-black tracking-tight text-slate-900">
          Owner & Agency Identity Verification
        </h1>
        <p className="text-xs text-slate-500 leading-relaxed">
          In compliance with Indian Income Tax rules, GST regulations, and automated banking settlements, verify your tax identity and payout account prior to publishing listings.
        </p>
      </div>

      <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-xs text-amber-900 flex items-start gap-3">
        <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
        <div>
          <strong className="block text-amber-950 font-bold">Mandatory Prior to Property Listing</strong>
          <p className="text-[11px] text-amber-800 mt-0.5">
            Rent collected from tenants will settle directly into this verified bank account. Legal business and trade details will be stamped on rental agreements.
          </p>
        </div>
      </div>

      {/* STEP 1: PAN Verification */}
      <div className="p-6 rounded-3xl bg-white border border-slate-200 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-900 flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-blue-600" /> 1. Income Tax PAN Card Verification
          </span>
          {isPanVerified ? (
            <Badge variant="success" dot>
              PAN VERIFIED
            </Badge>
          ) : (
            <Badge variant="warning">REQUIRED</Badge>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label="PAN Number *"
            placeholder="e.g. ABCDE1234F"
            value={pan}
            onChange={(e) => setPan(e.target.value.toUpperCase())}
            disabled={isPanVerified}
            maxLength={10}
            className="font-mono uppercase font-bold"
          />
          <Input
            label="Registered Legal Name"
            placeholder="Name matching PAN record"
            value={name}
            onChange={(e) => setName(e.target.value)}
            disabled={isPanVerified}
          />
        </div>

        {!isPanVerified ? (
          <Button
            type="button"
            onClick={handleVerifyPan}
            isLoading={isPanLoading}
            className="w-full text-xs font-bold"
          >
            Verify PAN with Income Tax Database ➔
          </Button>
        ) : (
          <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-900 space-y-1">
            <p className="font-bold flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              {panDetails?.registeredName || name}
            </p>
            <p className="text-[11px] text-emerald-700">
              Entity Category: {panDetails?.entityType || 'Business / Individual'} • Verified via Cashfree
            </p>
          </div>
        )}

        {/* Auto-Discovered GSTIN Selector */}
        {discoveredGstins.length > 0 && (
          <div className="p-4 rounded-2xl bg-indigo-50/70 border border-indigo-200/80 space-y-2.5 animate-in fade-in">
            <div className="flex items-center gap-2 text-xs font-bold text-indigo-950">
              <Sparkles className="w-4 h-4 text-indigo-600" />
              <span>Smart Auto-Discovery: We found {discoveredGstins.length} GSTIN(s) linked to this PAN!</span>
            </div>
            <p className="text-[11px] text-indigo-800">
              Select your business GSTIN below to automatically verify and pull official trade details:
            </p>
            <div className="flex flex-wrap gap-2 pt-1">
              {discoveredGstins.map((item) => (
                <button
                  key={item.gstin}
                  type="button"
                  onClick={() => handleVerifyGstin(item.gstin)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-mono font-bold border transition-all flex items-center gap-2 ${
                    gstin === item.gstin && isGstinVerified
                      ? 'bg-emerald-600 text-white border-emerald-600 shadow-xs'
                      : 'bg-white text-indigo-950 border-indigo-200 hover:border-indigo-400 hover:bg-indigo-50'
                  }`}
                >
                  <span>{item.gstin}</span>
                  <span className="text-[10px] font-sans font-semibold opacity-75">
                    ({item.state || 'Active'})
                  </span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* STEP 2: GSTIN Government Business Verification */}
      <div className="p-6 rounded-3xl bg-white border border-slate-200 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-900 flex items-center gap-2">
            <Building2 className="w-4 h-4 text-indigo-600" /> 2. GSTIN Government Business Registry (KYB)
          </span>
          {isGstinVerified ? (
            <Badge variant="success" dot>
              GST VERIFIED
            </Badge>
          ) : (
            <Badge variant="info">OPTIONAL / RECOMMENDED</Badge>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 items-end">
          <div className="sm:col-span-2">
            <Input
              label="GSTIN Number (15 Characters)"
              placeholder="e.g. 29ABCDE1234F1Z5"
              value={gstin}
              onChange={(e) => setGstin(e.target.value.toUpperCase())}
              disabled={isGstinVerified}
              maxLength={15}
              className="font-mono uppercase font-bold"
              helperText="Auto-fills official legal name, brand name, and registered address."
            />
          </div>
          <div>
            {!isGstinVerified ? (
              <Button
                type="button"
                onClick={() => handleVerifyGstin()}
                isLoading={isGstinLoading}
                className="w-full text-xs font-bold"
              >
                Verify GSTIN ➔
              </Button>
            ) : (
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setIsGstinVerified(false);
                  setGstinDetails(null);
                }}
                className="w-full text-xs"
              >
                Change GSTIN
              </Button>
            )}
          </div>
        </div>

        {/* GSTIN Government Details Card */}
        {isGstinVerified && gstinDetails && (
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2 text-xs">
            <div className="flex items-center justify-between border-b border-slate-200 pb-2">
              <span className="font-bold text-slate-900 text-sm">
                {gstinDetails.legalName || 'Registered Enterprise'}
              </span>
              <Badge variant="success">{gstinDetails.status || 'Active'}</Badge>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px] text-slate-600 pt-1">
              <div>
                <span className="font-semibold text-slate-500 block">Trade / Brand Name:</span>
                <span className="text-slate-900 font-medium">{gstinDetails.tradeName || 'Same as Legal Name'}</span>
              </div>
              <div>
                <span className="font-semibold text-slate-500 block">Constitution:</span>
                <span className="text-slate-900 font-medium">{gstinDetails.constitution || gstinDetails.taxpayerType || 'Regular'}</span>
              </div>
              <div className="sm:col-span-2">
                <span className="font-semibold text-slate-500 block">Registered Office Address:</span>
                <span className="text-slate-900 font-medium flex items-start gap-1 mt-0.5">
                  <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
                  {gstinDetails.address || 'Address on record'}
                </span>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* STEP 3: Corporate CIN / MCA Verification (Optional) */}
      <div className="p-6 rounded-3xl bg-white border border-slate-200 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-900 flex items-center gap-2">
            <Briefcase className="w-4 h-4 text-purple-600" /> 3. Company CIN / MCA Verification (Optional)
          </span>
          {isCinVerified ? (
            <Badge variant="success" dot>
              MCA VERIFIED
            </Badge>
          ) : (
            <button
              type="button"
              onClick={() => setShowCinSection(!showCinSection)}
              className="text-xs text-blue-600 hover:text-blue-800 font-semibold"
            >
              {showCinSection ? 'Hide' : '+ For Pvt Ltd / LLP'}
            </button>
          )}
        </div>

        {(showCinSection || isCinVerified) && (
          <div className="space-y-3 pt-1">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 items-end">
              <div className="sm:col-span-2">
                <Input
                  label="Corporate Identification Number (CIN)"
                  placeholder="e.g. U72900KA2015PTC082988"
                  value={cin}
                  onChange={(e) => setCin(e.target.value.toUpperCase())}
                  disabled={isCinVerified}
                  maxLength={21}
                  className="font-mono uppercase font-bold"
                  helperText="Verifies company registration against Ministry of Corporate Affairs (MCA)."
                />
              </div>
              <div>
                {!isCinVerified ? (
                  <Button
                    type="button"
                    onClick={handleVerifyCin}
                    isLoading={isCinLoading}
                    className="w-full text-xs font-bold"
                  >
                    Verify MCA ➔
                  </Button>
                ) : (
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setIsCinVerified(false);
                      setCinDetails(null);
                    }}
                    className="w-full text-xs"
                  >
                    Change CIN
                  </Button>
                )}
              </div>
            </div>

            {isCinVerified && cinDetails && (
              <div className="p-3.5 rounded-2xl bg-purple-50 border border-purple-200 text-xs text-purple-950 space-y-1">
                <p className="font-bold">{cinDetails.companyName}</p>
                <p className="text-[11px] text-purple-800">
                  Status: {cinDetails.companyStatus || 'Active'} • Incorporated: {cinDetails.incorporationDate || 'N/A'} • Reg #: {cinDetails.registrationNumber}
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* STEP 4: Rent Settlement Bank Account */}
      <div className="space-y-3">
        <span className="text-xs font-bold uppercase tracking-wider text-slate-900 flex items-center gap-2 px-1">
          <CreditCard className="w-4 h-4 text-emerald-600" /> 4. Rent Settlement Payout Account
        </span>

        <BankVerificationCard
          initialAccountHolderName={gstinDetails?.legalName || panDetails?.registeredName || name}
          onVerified={(result: BankVerificationResult) => {
            if (result.accountNumber) setBankAccount(result.accountNumber);
            if (result.ifsc) setIfsc(result.ifsc);
            if (result.bankName) setBankName(result.bankName);
            setIsBankVerified(true);
          }}
          currentData={{
            accountNumber: bankAccount,
            ifsc: ifsc,
            bankName: bankName,
            accountHolderName: gstinDetails?.legalName || panDetails?.registeredName || name,
            isVerified: isBankVerified,
          }}
        />

        <p className="text-[11px] text-slate-500 px-1">
          Monthly tenant rent payments and security deposit credits will settle directly into this verified account.
        </p>
      </div>

      {/* Footer Actions */}
      <div className="flex items-center justify-between pt-2">
        <Button type="button" variant="outline" onClick={() => router.push('/app/properties')}>
          Cancel
        </Button>
        <Button
          type="button"
          onClick={handleCompleteSetup}
          disabled={!isPanVerified || isSubmitting}
          isLoading={isSubmitting}
          className="font-bold px-6 shadow-md shadow-blue-500/20"
        >
          Save & Proceed to Property Listing ➔
        </Button>
      </div>
    </div>
  );
}
