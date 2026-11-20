'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { Settings, Save, Building, ShieldCheck, Banknote, RefreshCw, Globe, ExternalLink, CheckCircle2, Clock, Sparkles } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { orgApi, FullBrandingSettings } from '@/lib/api/orgApi';
import { kybApi } from '@/lib/api/kybApi';
import { OrganizationType } from '@/lib/auth/authTypes';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { getErrorMessage } from '@/lib/utils';
import { validatePan, validateGstin, validateIfsc } from '@/lib/validation';
import { useToast } from '@/lib/toast/ToastContext';
import { BankVerificationCard } from '@/components/verification/BankVerificationCard';

export default function SettingsPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const [orgType, setOrgType] = useState<OrganizationType>('HYBRID');
  const [customDomain, setCustomDomain] = useState('');
  const [domainStatus, setDomainStatus] = useState('PENDING_DNS');
  const [orgSlug, setOrgSlug] = useState('');
  const [isSavingDomain, setIsSavingDomain] = useState(false);
  const [isVerifyingGstin, setIsVerifyingGstin] = useState(false);
  const [gstinVerifiedBadge, setGstinVerifiedBadge] = useState<string | null>(null);
  const [branding, setBranding] = useState<FullBrandingSettings>({
    legalBusinessName: '',
    tradeName: '',
    ownerPan: '',
    ownerGstin: '',
    reraNumber: '',
    registeredOfficeAddress: '',
    contactPhone: '',
    contactEmail: '',
    primaryColor: '#2563eb',
    secondaryColor: '#1e293b',
    ownerUpiId: '',
    bankAccountNumber: '',
    bankIfscCode: '',
    bankName: '',
    accountHolderName: '',
  });

  const [isLoading, setIsLoading] = useState(true);
  const [isSavingBranding, setIsSavingBranding] = useState(false);
  const [isUpdatingType, setIsUpdatingType] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!user?.organizationId) return;
    const fetchOrg = async () => {
      setIsLoading(true);
      try {
        const [orgDetails, fullBranding] = await Promise.all([
          orgApi.getOrganizationDetails(user.organizationId),
          orgApi.getFullBranding(user.organizationId).catch(() => null),
        ]);
        if (orgDetails.type) setOrgType(orgDetails.type);
        if (orgDetails.slug) setOrgSlug(orgDetails.slug);
        if (orgDetails.customDomain) setCustomDomain(orgDetails.customDomain);
        if (orgDetails.domainStatus) setDomainStatus(orgDetails.domainStatus);
        if (fullBranding) {
          setBranding(fullBranding);
        } else if (orgDetails.branding) {
          setBranding(orgDetails.branding);
        } else {
          setBranding((prev) => ({ ...prev, legalBusinessName: orgDetails.name }));
        }
      } catch (err) {
        console.error('Failed to load branding', err);
      } finally {
        setIsLoading(false);
      }
    };
    fetchOrg();
  }, [user?.organizationId]);

  const handleSaveDomain = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.organizationId) return;
    setIsSavingDomain(true);
    try {
      const res = await orgApi.updateCustomDomain(user.organizationId, customDomain.trim() || null);
      setCustomDomain(res.customDomain || '');
      setDomainStatus(res.domainStatus || 'PENDING_DNS');
      toast.success('Domain settings updated successfully!');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to update custom domain.'));
    } finally {
      setIsSavingDomain(false);
    }
  };

  const handleVerifyGstin = async () => {
    if (!branding.ownerGstin || !validateGstin(branding.ownerGstin)) {
      toast.warning('Please enter a valid 15-character GSTIN (e.g. 29ABCDE1234F1Z5).');
      return;
    }
    setIsVerifyingGstin(true);
    try {
      const data = await kybApi.verifyGstin(branding.ownerGstin);
      if (data.valid) {
        setGstinVerifiedBadge(data.taxpayerType || data.constitution || 'Active');
        setBranding((prev) => ({
          ...prev,
          legalBusinessName: data.legalName || prev.legalBusinessName,
          tradeName: data.tradeName || prev.tradeName,
          registeredOfficeAddress: data.address || prev.registeredOfficeAddress,
        }));
        toast.success(`GSTIN Verified: ${data.legalName || data.tradeName}! Business details updated.`);
      } else {
        toast.error(`GSTIN Verification Failed: ${data.message || 'Invalid GSTIN'}`);
      }
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to verify GSTIN with Cashfree.'));
    } finally {
      setIsVerifyingGstin(false);
    }
  };

  const handleSaveBranding = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.organizationId) return;

    if (branding.ownerPan && !validatePan(branding.ownerPan)) {
      toast.warning('Invalid PAN card number format. Example: ABCDE1234F');
      return;
    }
    if (branding.ownerGstin && !validateGstin(branding.ownerGstin)) {
      toast.warning('Invalid GSTIN number format. Example: 29ABCDE1234F1Z5');
      return;
    }
    if (branding.bankIfscCode && !validateIfsc(branding.bankIfscCode)) {
      toast.warning('Invalid IFSC Code format. Example: SBIN0001234');
      return;
    }

    setIsSavingBranding(true);
    setSuccessMessage(null);
    try {
      await orgApi.updateBranding(user.organizationId, branding);
      toast.success('Branding & Organization bank details updated successfully!');
      setSuccessMessage('Branding & Organization bank details updated successfully!');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to update branding settings.'));
    } finally {
      setIsSavingBranding(false);
    }
  };

  const handleUpdateOrgType = async (newType: OrganizationType) => {
    if (!user?.organizationId || newType === orgType) return;
    if (!confirm(`Are you sure you want to switch your organization operating mode to ${newType}?`)) return;

    setIsUpdatingType(true);
    setSuccessMessage(null);
    try {
      await orgApi.updateOrgType(user.organizationId, newType);
      setOrgType(newType);
      toast.success(`Organization operating mode updated to ${newType}!`);
      setSuccessMessage(`Organization operating mode updated to ${newType}! Please refresh to update navigation.`);
      window.location.reload();
    } catch (err) {
      toast.error(getErrorMessage(err, 'Cannot switch organization type. Check active landlords/properties constraints.'));
    } finally {
      setIsUpdatingType(false);
    }
  };

  if (isLoading) {
    return <div className="text-center py-12 text-slate-400 text-sm">Loading settings...</div>;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Organization Settings</h1>
        <p className="text-xs text-slate-500 mt-1">
          Configure operating model (Owner vs Brokerage), legal entity details, bank account info, and portal branding.
        </p>
      </div>

      {successMessage && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs rounded-xl font-medium">
          {successMessage}
        </div>
      )}

      {/* Operating Model Switcher */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs space-y-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
            <Building className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-slate-900">Organization Operating Model</h2>
            <p className="text-xs text-slate-500">Controls which features (Landlords directory, brokerage fees, payouts) are enabled.</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
          <button
            type="button"
            onClick={() => handleUpdateOrgType('OWNER')}
            disabled={isUpdatingType}
            className={`p-4 rounded-xl text-left border-2 transition-all ${
              orgType === 'OWNER'
                ? 'border-blue-600 bg-blue-50/50 shadow-xs'
                : 'border-slate-200 hover:border-slate-300 bg-slate-50/30'
            }`}
          >
            <span className="font-bold text-xs text-slate-900 block">Direct Property Owner</span>
            <span className="text-[11px] text-slate-500 block mt-1">
              Own & manage your properties directly. Hides external landlord directory and payout management.
            </span>
          </button>

          <button
            type="button"
            onClick={() => handleUpdateOrgType('BROKERAGE')}
            disabled={isUpdatingType}
            className={`p-4 rounded-xl text-left border-2 transition-all ${
              orgType === 'BROKERAGE'
                ? 'border-blue-600 bg-blue-50/50 shadow-xs'
                : 'border-slate-200 hover:border-slate-300 bg-slate-50/30'
            }`}
          >
            <span className="font-bold text-xs text-slate-900 block">Brokerage / Agency</span>
            <span className="text-[11px] text-slate-500 block mt-1">
              Manage properties on behalf of external landlords. Requires landlord assignment & enables payouts.
            </span>
          </button>

          <button
            type="button"
            onClick={() => handleUpdateOrgType('HYBRID')}
            disabled={isUpdatingType}
            className={`p-4 rounded-xl text-left border-2 transition-all ${
              orgType === 'HYBRID'
                ? 'border-blue-600 bg-blue-50/50 shadow-xs'
                : 'border-slate-200 hover:border-slate-300 bg-slate-50/30'
            }`}
          >
            <span className="font-bold text-xs text-slate-900 block">Hybrid / Mixed Model</span>
            <span className="text-[11px] text-slate-500 block mt-1">
              Combination of self-owned properties and third-party managed properties.
            </span>
          </button>
        </div>
      </div>

      {/* Website & Custom Domain Settings */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs space-y-5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
              <Globe className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-slate-900">Dedicated Website & Custom Domain</h2>
              <p className="text-xs text-slate-500">Every owner and agency gets their own branded website showcasing their properties.</p>
            </div>
          </div>
          {customDomain && (
            <Badge variant={domainStatus === 'ACTIVE' ? 'success' : 'warning'}>
              {domainStatus === 'ACTIVE' ? (
                <><CheckCircle2 className="w-3 h-3 mr-1 inline" /> Domain Active</>
              ) : (
                <><Clock className="w-3 h-3 mr-1 inline" /> DNS Pending</>
              )}
            </Badge>
          )}
        </div>

        {/* Free Subdomain Preview */}
        <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">Your Included StayFile Website URL</span>
            <span className="text-sm font-semibold text-slate-900 flex items-center gap-1.5 mt-0.5">
              https://{orgSlug || 'agency'}.stayfile.in
            </span>
          </div>
          {orgSlug && (
            <Link
              href={`/p/${orgSlug}`}
              target="_blank"
              className="inline-flex items-center text-xs font-semibold text-blue-600 hover:text-blue-700 bg-white border border-slate-200 px-3 py-1.5 rounded-lg shadow-2xs hover:bg-slate-50 transition-colors"
            >
              View Public Website <ExternalLink className="w-3 h-3 ml-1" />
            </Link>
          )}
        </div>

        {/* Custom Domain Form */}
        <form onSubmit={handleSaveDomain} className="space-y-4 pt-2">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
            <div className="md:col-span-2">
              <Input
                label="Custom Branded Domain (Optional)"
                placeholder="e.g. www.royalstays.com or stays.mybrand.in"
                value={customDomain}
                onChange={(e) => setCustomDomain(e.target.value)}
                helperText="Connect your own root domain or subdomain to replace the default stayfile.in URL."
              />
            </div>
            <div className="flex gap-2">
              <Button type="submit" isLoading={isSavingDomain} className="w-full">
                <Save className="w-4 h-4 mr-2" /> Save Domain
              </Button>
            </div>
          </div>

          {/* DNS Configuration Helper */}
          <div className="p-3.5 bg-blue-50/70 border border-blue-100 rounded-xl text-xs text-blue-900 space-y-1.5">
            <p className="font-semibold text-blue-950">DNS Setup Instructions:</p>
            <p className="text-blue-800">
              Add a <strong>CNAME</strong> record at your domain registrar (GoDaddy, Namecheap, Cloudflare, etc.):
            </p>
            <div className="bg-white/80 p-2 rounded-lg font-mono text-[11px] text-slate-800 border border-blue-200/60 flex flex-wrap gap-x-6 gap-y-1">
              <span><strong>Type:</strong> CNAME</span>
              <span><strong>Host:</strong> {customDomain ? customDomain.split('.')[0] : 'www'}</span>
              <span><strong>Points to:</strong> cname.stayfile.in</span>
            </div>
          </div>
        </form>
      </div>

      <form onSubmit={handleSaveBranding} className="bg-white rounded-2xl border border-slate-200 p-8 shadow-xs space-y-6">
        <div className="space-y-4">
          <h2 className="text-sm font-bold text-slate-900 uppercase tracking-wider text-blue-600">
            Legal & Trade Information
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Legal Business Name *"
              value={branding.legalBusinessName || ''}
              onChange={(e) => setBranding({ ...branding, legalBusinessName: e.target.value })}
              required
            />
            <Input
              label="Trade Name / Brand"
              value={branding.tradeName || ''}
              onChange={(e) => setBranding({ ...branding, tradeName: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Input
              label="Owner PAN"
              placeholder="ABCDE1234F"
              value={branding.ownerPan || ''}
              onChange={(e) => setBranding({ ...branding, ownerPan: e.target.value })}
            />
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-xs font-semibold text-slate-700">GSTIN</label>
                {gstinVerifiedBadge && (
                  <Badge variant="success" dot className="text-[10px]">
                    Verified • {gstinVerifiedBadge}
                  </Badge>
                )}
              </div>
              <div className="flex gap-2">
                <Input
                  placeholder="29ABCDE1234F1Z5"
                  value={branding.ownerGstin || ''}
                  onChange={(e) => {
                    setBranding({ ...branding, ownerGstin: e.target.value.toUpperCase() });
                    setGstinVerifiedBadge(null);
                  }}
                  maxLength={15}
                  className="font-mono uppercase font-bold"
                />
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleVerifyGstin}
                  isLoading={isVerifyingGstin}
                  className="text-xs shrink-0"
                  title="Verify GSTIN with Government Registry via Cashfree"
                >
                  <Sparkles className="w-3.5 h-3.5 mr-1 text-indigo-600" /> Verify
                </Button>
              </div>
            </div>
            <Input
              label="RERA Registration #"
              placeholder="PRM/KA/RERA/..."
              value={branding.reraNumber || ''}
              onChange={(e) => setBranding({ ...branding, reraNumber: e.target.value })}
            />
          </div>
        </div>

        <hr className="border-slate-100" />

        {/* Bank & Payment Details for Direct Owner Collections */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold text-slate-900 uppercase tracking-wider text-blue-600">
                Owner Collection Bank Account & UPI
              </h2>
              <p className="text-xs text-slate-500">
                Used on invoices and payout settlements for self-owned properties.
              </p>
            </div>
            {branding.bankAccountNumber && (
              <Badge variant="success" dot>
                BANK VERIFIED
              </Badge>
            )}
          </div>

          <BankVerificationCard
            initialAccountHolderName={branding.accountHolderName || branding.legalBusinessName}
            onVerified={(result) => {
              setBranding((prev) => ({
                ...prev,
                bankAccountNumber: result.accountNumber || prev.bankAccountNumber,
                bankIfscCode: result.ifsc || prev.bankIfscCode,
                bankName: result.bankName || prev.bankName,
                accountHolderName: result.accountHolderName || prev.accountHolderName,
                ownerUpiId: result.upiId || prev.ownerUpiId,
              }));
              toast.success('Bank account verified and updated in settings!');
            }}
            currentData={{
              accountNumber: branding.bankAccountNumber,
              ifsc: branding.bankIfscCode,
              bankName: branding.bankName,
              accountHolderName: branding.accountHolderName,
              upiId: branding.ownerUpiId,
              isVerified: Boolean(branding.bankAccountNumber && branding.bankIfscCode),
            }}
          />
        </div>

        <hr className="border-slate-100" />

        <div className="space-y-4">
          <h2 className="text-sm font-bold text-slate-900 uppercase tracking-wider text-blue-600">
            Contact & Office Details
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Support Phone"
              value={branding.contactPhone || ''}
              onChange={(e) => setBranding({ ...branding, contactPhone: e.target.value })}
            />
            <Input
              label="Support Email"
              type="email"
              value={branding.contactEmail || ''}
              onChange={(e) => setBranding({ ...branding, contactEmail: e.target.value })}
            />
          </div>

          <Input
            label="Registered Office Address"
            value={branding.registeredOfficeAddress || ''}
            onChange={(e) => setBranding({ ...branding, registeredOfficeAddress: e.target.value })}
          />
        </div>

        <hr className="border-slate-100" />

        <div className="space-y-4">
          <h2 className="text-sm font-bold text-slate-900 uppercase tracking-wider text-blue-600">
            Brand Colors & Logo
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Logo Image URL"
              placeholder="https://..."
              value={branding.agencyLogoUrl || ''}
              onChange={(e) => setBranding({ ...branding, agencyLogoUrl: e.target.value })}
            />
            <div className="grid grid-cols-2 gap-3">
              <Input
                label="Primary Color"
                type="color"
                className="h-10 p-1 cursor-pointer"
                value={branding.primaryColor || '#2563eb'}
                onChange={(e) => setBranding({ ...branding, primaryColor: e.target.value })}
              />
              <Input
                label="Secondary Color"
                type="color"
                className="h-10 p-1 cursor-pointer"
                value={branding.secondaryColor || '#1e293b'}
                onChange={(e) => setBranding({ ...branding, secondaryColor: e.target.value })}
              />
            </div>
          </div>
        </div>

        <div className="pt-4 flex justify-end">
          <Button type="submit" isLoading={isSavingBranding}>
            <Save className="w-4 h-4 mr-2" /> Save Settings & Bank Info
          </Button>
        </div>
      </form>
    </div>
  );
}

