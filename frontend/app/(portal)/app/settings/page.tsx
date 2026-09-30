'use client';

import React, { useEffect, useState } from 'react';
import { Settings, Save, Building, ShieldCheck } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { orgApi, BrandingSettings } from '@/lib/api/orgApi';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';

export default function SettingsPage() {
  const { user } = useAuth();
  const [branding, setBranding] = useState<BrandingSettings>({
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
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!user?.organizationId) return;
    const fetchOrg = async () => {
      setIsLoading(true);
      try {
        const data = await orgApi.getOrganizationDetails(user.organizationId);
        if (data.branding) {
          setBranding(data.branding);
        } else {
          setBranding((prev) => ({ ...prev, legalBusinessName: data.name }));
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
    setIsSaving(true);
    setSuccessMessage(null);
    try {
      await orgApi.updateBranding(user.organizationId, branding);
      setSuccessMessage('Branding & Organization details updated successfully!');
    } catch (err) {
      alert('Failed to update branding settings.');
    } finally {
      setIsSaving(false);
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
          Configure legal business name, agency branding, logo, colors, GSTIN, RERA registration, and contact details.
        </p>
      </div>

      {successMessage && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs rounded-xl font-medium">
          {successMessage}
        </div>
      )}

      <form onSubmit={handleSaveBranding} className="bg-white rounded-2xl border border-slate-200 p-8 shadow-xs space-y-6">
        <div className="space-y-4">
          <h2 className="text-sm font-bold text-slate-900 uppercase tracking-wider text-blue-600">
            Legal & Trade Information
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Legal Business Name"
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
          <Button type="submit" isLoading={isSaving}>
            <Save className="w-4 h-4 mr-2" /> Save Branding Settings
          </Button>
        </div>
      </form>
    </div>
  );
}
