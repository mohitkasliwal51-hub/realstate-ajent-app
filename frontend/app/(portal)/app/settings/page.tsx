'use client';

import React, { useEffect, useState } from 'react';
import { Settings, Save, Building, ShieldCheck, Banknote, RefreshCw } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { orgApi, FullBrandingSettings } from '@/lib/api/orgApi';
import { OrganizationType } from '@/lib/auth/authTypes';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { getErrorMessage } from '@/lib/utils';

export default function SettingsPage() {
  const { user } = useAuth();
  const [orgType, setOrgType] = useState<OrganizationType>('HYBRID');
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

  const handleSaveBranding = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.organizationId) return;
    setIsSavingBranding(true);
    setSuccessMessage(null);
    try {
      await orgApi.updateBranding(user.organizationId, branding);
      setSuccessMessage('Branding & Organization bank details updated successfully!');
    } catch (err) {
      alert(getErrorMessage(err, 'Failed to update branding settings.'));
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
      setSuccessMessage(`Organization operating mode updated to ${newType}! Please refresh to update navigation.`);
      window.location.reload();
    } catch (err) {
      alert(getErrorMessage(err, 'Cannot switch organization type. Check active landlords/properties constraints.'));
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
            <Input
              label="GSTIN"
              placeholder="29ABCDE1234F1Z5"
              value={branding.ownerGstin || ''}
              onChange={(e) => setBranding({ ...branding, ownerGstin: e.target.value })}
            />
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
          <h2 className="text-sm font-bold text-slate-900 uppercase tracking-wider text-blue-600">
            Owner Collection Bank Account & UPI
          </h2>
          <p className="text-xs text-slate-500">
            Used on invoices and payment receipts for self-owned properties.
          </p>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Account Holder Name"
              placeholder="e.g. Acme Residencies Private Limited"
              value={branding.accountHolderName || ''}
              onChange={(e) => setBranding({ ...branding, accountHolderName: e.target.value })}
            />
            <Input
              label="Bank Account Number"
              placeholder="1234567890"
              value={branding.bankAccountNumber || ''}
              onChange={(e) => setBranding({ ...branding, bankAccountNumber: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Input
              label="Bank Name"
              placeholder="HDFC Bank"
              value={branding.bankName || ''}
              onChange={(e) => setBranding({ ...branding, bankName: e.target.value })}
            />
            <Input
              label="IFSC Code"
              placeholder="HDFC0001234"
              value={branding.bankIfscCode || ''}
              onChange={(e) => setBranding({ ...branding, bankIfscCode: e.target.value })}
            />
            <Input
              label="Owner UPI ID"
              placeholder="acme@hdfcbank"
              value={branding.ownerUpiId || ''}
              onChange={(e) => setBranding({ ...branding, ownerUpiId: e.target.value })}
            />
          </div>
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

