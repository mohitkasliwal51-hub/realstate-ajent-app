'use client';

import React, { useEffect, useState } from 'react';
import { Users, Plus, Mail, Phone } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { tenantApi, Tenant } from '@/lib/api/tenantApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';

export default function TenantsPage() {
  const { user } = useAuth();
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    emergencyContactName: '',
    emergencyContactPhone: '',
    permanentAddress: '',
    idProofType: 'Aadhaar Card',
    idProofNumber: '',
  });

  const loadTenants = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      const data = await tenantApi.getTenants(user.organizationId);
      setTenants(data || []);
    } catch (err) {
      console.error('Failed to load tenants', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadTenants();
  }, [user?.organizationId]);

  const handleCreateTenant = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.organizationId) return;
    setIsSubmitting(true);
    try {
      await tenantApi.createTenant({
        organizationId: user.organizationId,
        ownerId: user.id,
        ...formData,
      });
      setIsModalOpen(false);
      setFormData({
        fullName: '',
        email: '',
        phone: '',
        emergencyContactName: '',
        emergencyContactPhone: '',
        permanentAddress: '',
        idProofType: 'Aadhaar Card',
        idProofNumber: '',
      });
      await loadTenants();
    } catch (err) {
      alert('Failed to register tenant.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getKycBadge = (isIdVerified: boolean) => {
    return isIdVerified
      ? <Badge variant="success">KYC Verified</Badge>
      : <Badge variant="warning">KYC Pending</Badge>;
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Tenants & Onboarding</h1>
          <p className="text-xs text-slate-500 mt-1">
            Guided onboarding, emergency contacts, permanent address details, and ID verification status.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>
          <Plus className="w-4 h-4 mr-2" /> Register Tenant
        </Button>
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-slate-400 text-sm">Loading tenants...</div>
      ) : tenants.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-slate-300 space-y-4">
          <div className="w-12 h-12 rounded-full bg-purple-50 text-purple-600 flex items-center justify-center mx-auto">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <h3 className="font-bold text-slate-900">No tenants registered yet</h3>
            <p className="text-xs text-slate-500 mt-1">Onboard your first tenant to start lease agreements.</p>
          </div>
          <Button onClick={() => setIsModalOpen(true)}>
            <Plus className="w-4 h-4 mr-2" /> Register Tenant
          </Button>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-semibold">
                <tr>
                  <th className="px-6 py-3.5">Tenant Name</th>
                  <th className="px-6 py-3.5">Contact Info</th>
                  <th className="px-6 py-3.5">ID Proof & Address</th>
                  <th className="px-6 py-3.5">KYC Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {tenants.map((tenant) => (
                  <tr key={tenant.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-purple-100 text-purple-700 font-bold flex items-center justify-center text-xs">
                          {tenant.fullName.charAt(0)}
                        </div>
                        <div>
                          <p className="font-bold text-slate-900 text-sm">{tenant.fullName}</p>
                          {tenant.emergencyContactName && (
                            <p className="text-[11px] text-slate-400">
                              Emergency: {tenant.emergencyContactName} ({tenant.emergencyContactPhone})
                            </p>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 space-y-1">
                      <div className="flex items-center gap-1.5 text-slate-600">
                        <Mail className="w-3.5 h-3.5 text-slate-400" /> {tenant.email}
                      </div>
                      <div className="flex items-center gap-1.5 text-slate-600">
                        <Phone className="w-3.5 h-3.5 text-slate-400" /> {tenant.phone}
                      </div>
                    </td>
                    <td className="px-6 py-4 space-y-1">
                      {tenant.idProofLast4 && (
                        <p className="text-slate-700 font-semibold">
                          {tenant.idProofType}: ****{tenant.idProofLast4}
                        </p>
                      )}
                      {tenant.permanentAddress && (
                        <p className="text-slate-500 truncate max-w-xs">{tenant.permanentAddress}</p>
                      )}
                    </td>
                    <td className="px-6 py-4">{getKycBadge(tenant.isIdVerified)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add Tenant Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Guided Tenant Onboarding"
        description="Step-by-step basic details, emergency contact & KYC info"
      >
        <form onSubmit={handleCreateTenant} className="space-y-4">
          <Input
            label="1. Full Name"
            placeholder="e.g. Rahul Sharma"
            value={formData.fullName}
            onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
            required
          />

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Email Address"
              type="email"
              placeholder="rahul@example.com"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              required
            />
            <Input
              label="Phone Number"
              type="tel"
              placeholder="+91 9876543210"
              value={formData.phone}
              onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="2. Emergency Contact Name"
              placeholder="Parent / Guardian"
              value={formData.emergencyContactName}
              onChange={(e) => setFormData({ ...formData, emergencyContactName: e.target.value })}
            />
            <Input
              label="Emergency Phone"
              placeholder="+91 9876543210"
              value={formData.emergencyContactPhone}
              onChange={(e) => setFormData({ ...formData, emergencyContactPhone: e.target.value })}
            />
          </div>

          <Input
            label="3. Permanent Address"
            placeholder="Home address details"
            value={formData.permanentAddress}
            onChange={(e) => setFormData({ ...formData, permanentAddress: e.target.value })}
          />

          <div className="grid grid-cols-2 gap-3">
            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                4. ID Proof Type
              </label>
              <select
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={formData.idProofType}
                onChange={(e) => setFormData({ ...formData, idProofType: e.target.value })}
              >
                <option value="Aadhaar Card">Aadhaar Card</option>
                <option value="PAN Card">PAN Card</option>
                <option value="Passport">Passport</option>
                <option value="Voter ID">Voter ID</option>
                <option value="Driving License">Driving License</option>
              </select>
            </div>

            <Input
              label="ID Proof Number"
              placeholder="e.g. 1234-5678-9012"
              value={formData.idProofNumber}
              onChange={(e) => setFormData({ ...formData, idProofNumber: e.target.value })}
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Complete Tenant Onboarding
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
