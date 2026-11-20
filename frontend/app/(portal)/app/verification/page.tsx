'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  ShieldCheck,
  CreditCard,
  Building,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  ArrowLeft,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { kycApi } from '@/lib/api/kycApi';
import { useToast } from '@/lib/toast/ToastContext';

export default function OwnerVerificationPage() {
  const router = useRouter();
  const { toast } = useToast();

  const [pan, setPan] = useState('');
  const [name, setName] = useState('');
  const [bankAccount, setBankAccount] = useState('');
  const [ifsc, setIfsc] = useState('');
  const [gstin, setGstin] = useState('');

  const [isLoading, setIsLoading] = useState(false);
  const [isPanVerified, setIsPanVerified] = useState(false);
  const [verifiedDetails, setVerifiedDetails] = useState<{
    registeredName?: string;
    entityType?: string;
    score?: string;
  } | null>(null);

  const handleVerifyPan = async () => {
    if (!pan || pan.trim().length !== 10) {
      toast.warning('Please enter a valid 10-character PAN number.');
      return;
    }

    setIsLoading(true);
    try {
      const data = await kycApi.verifyPan(pan, name);
      if (data.valid) {
        setIsPanVerified(true);
        setVerifiedDetails({
          registeredName: data.registeredName,
          entityType: data.type,
          score: data.nameMatchScore,
        });
        toast.success(`PAN Verified: ${data.registeredName}`);
      } else {
        toast.error(`PAN Verification Failed: ${data.message || 'Invalid PAN'}`);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to verify PAN with Cashfree';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCompleteSetup = () => {
    if (!isPanVerified) {
      toast.warning('Please verify your PAN number first.');
      return;
    }
    if (!bankAccount || !ifsc) {
      toast.warning('Please enter your rent settlement bank account and IFSC code.');
      return;
    }

    toast.success('Owner profile verified! You can now list properties.');
    router.push('/app/properties/new');
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex items-center justify-between">
        <Link
          href="/app/properties"
          className="text-xs font-semibold text-slate-500 hover:text-slate-800 flex items-center gap-1"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Properties
        </Link>
        <Badge variant="purple">Landlord Compliance</Badge>
      </div>

      <div className="space-y-2">
        <h1 className="text-2xl font-black tracking-tight text-slate-900">
          Owner & Agency Identity Verification
        </h1>
        <p className="text-xs text-slate-500 leading-relaxed">
          In compliance with Indian Income Tax rules and automated banking settlements, verify your tax identity and rent payout destination prior to publishing listings.
        </p>
      </div>

      <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-xs text-amber-900 flex items-start gap-3">
        <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
        <div>
          <strong className="block text-amber-950 font-bold">Mandatory Prior to Property Listing</strong>
          <p className="text-[11px] text-amber-800 mt-0.5">
            Rent collected from tenants will settle directly into this verified bank account.
          </p>
        </div>
      </div>

      {/* Step 1: PAN Verification */}
      <div className="p-6 rounded-3xl bg-white border border-slate-200 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-900 flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-blue-600" /> 1. Income Tax PAN Card Verification
          </span>
          {isPanVerified ? (
            <Badge variant="success" dot>
              VERIFIED
            </Badge>
          ) : (
            <Badge variant="warning">REQUIRED</Badge>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label="PAN Number"
            placeholder="e.g. ABCPV1234D"
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
            isLoading={isLoading}
            className="w-full text-xs font-bold"
          >
            Verify PAN with Income Tax Database ➔
          </Button>
        ) : (
          <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-900">
            <p className="font-bold">{verifiedDetails?.registeredName}</p>
            <p className="text-[11px] text-emerald-700">
              Entity: {verifiedDetails?.entityType || 'Individual / Business'} • Match: Verified with Cashfree
            </p>
          </div>
        )}
      </div>

      {/* Step 2: Rent Settlement Bank Account */}
      <div className="p-6 rounded-3xl bg-white border border-slate-200 shadow-sm space-y-4">
        <span className="text-xs font-bold uppercase tracking-wider text-slate-900 flex items-center gap-2">
          <CreditCard className="w-4 h-4 text-purple-600" /> 2. Rent Settlement Bank Account
        </span>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input
            label="Bank Account Number"
            placeholder="e.g. 026291800001191"
            value={bankAccount}
            onChange={(e) => setBankAccount(e.target.value)}
            required
          />
          <Input
            label="IFSC Code"
            placeholder="e.g. YESB0000262"
            value={ifsc}
            onChange={(e) => setIfsc(e.target.value.toUpperCase())}
            maxLength={11}
            className="font-mono uppercase font-bold"
            required
          />
        </div>

        <Input
          label="GSTIN (Optional for Agencies / Registered Firms)"
          placeholder="e.g. 29ABCDE1234F1Z5"
          value={gstin}
          onChange={(e) => setGstin(e.target.value.toUpperCase())}
          maxLength={15}
          className="font-mono uppercase"
        />

        <p className="text-[11px] text-slate-500">
          Monthly tenant rent payments and security deposit credits will settle directly into this account.
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
          disabled={!isPanVerified}
          className="font-bold px-6 shadow-md shadow-blue-500/20"
        >
          Unlock & Proceed to Property Listing ➔
        </Button>
      </div>
    </div>
  );
}
