'use client';

import React, { useEffect, useState } from 'react';
import { Briefcase, Plus, Phone, Mail, Building, Trash2, Edit, AlertCircle } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { landlordApi, Landlord, OwnerType } from '@/lib/api/landlordApi';
import { isBrokerMode } from '@/lib/orgMode';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { TableSkeleton } from '@/components/ui/TableSkeleton';
import { ConfirmModal } from '@/components/ui/ConfirmModal';
import {
  validateIndianPhone,
  validatePan,
  validateGstin,
  validateIfsc,
  normalizePan,
  normalizeGstin,
  normalizeIfsc,
  normalizeIndianPhone,
} from '@/lib/validation';
import { getErrorMessage } from '@/lib/utils';

export default function LandlordsPage() {
  const { user } = useAuth();
  const brokerMode = isBrokerMode(user);

  const [landlords, setLandlords] = useState<Landlord[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editingLandlord, setEditingLandlord] = useState<Landlord | null>(null);

  const [landlordToDelete, setLandlordToDelete] = useState<{ id: string; name: string } | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const [formData, setFormData] = useState({
    ownerType: 'INDIVIDUAL' as OwnerType,
    legalName: '',
    email: '',
    phone: '',
    pan: '',
    gstin: '',
    address: '',
    bankAccountNumber: '',
    bankIfscCode: '',
    bankName: '',
    accountHolderName: '',
    ownerUpiId: '',
  });

  const loadLandlords = async () => {
    if (!brokerMode) {
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    try {
      const data = await landlordApi.getLandlords();
      setLandlords(data || []);
    } catch (err) {
      console.error('Failed to load landlords', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadLandlords();
  }, [user?.organizationId]);

  const handleOpenAdd = () => {
    setEditingLandlord(null);
    setFormData({
      ownerType: 'INDIVIDUAL',
      legalName: '',
      email: '',
      phone: '',
      pan: '',
      gstin: '',
      address: '',
      bankAccountNumber: '',
      bankIfscCode: '',
      bankName: '',
      accountHolderName: '',
      ownerUpiId: '',
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (landlord: Landlord) => {
    setEditingLandlord(landlord);
    setFormData({
      ownerType: landlord.ownerType || 'INDIVIDUAL',
      legalName: landlord.legalName || '',
      email: landlord.email || '',
      phone: landlord.phone || '',
      pan: landlord.pan || '',
      gstin: landlord.gstin || '',
      address: landlord.address || '',
      bankAccountNumber: landlord.bankAccountNumber || '',
      bankIfscCode: landlord.bankIfscCode || '',
      bankName: landlord.bankName || '',
      accountHolderName: landlord.accountHolderName || '',
      ownerUpiId: landlord.ownerUpiId || '',
    });
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (formData.phone && !validateIndianPhone(formData.phone)) {
      alert('Please enter a valid 10-digit Indian phone number.');
      return;
    }
    if (formData.pan && !validatePan(formData.pan)) {
      alert('Please enter a valid 10-character PAN number (e.g., ABCDE1234F).');
      return;
    }
    if (formData.gstin && !validateGstin(formData.gstin)) {
      alert('Please enter a valid 15-character GSTIN (e.g., 07AAAAA0000A1Z5).');
      return;
    }
    if (formData.bankIfscCode && !validateIfsc(formData.bankIfscCode)) {
      alert('Please enter a valid 11-character IFSC code (e.g., SBIN0001234).');
      return;
    }

    const payload = {
      ...formData,
      phone: normalizeIndianPhone(formData.phone),
      pan: normalizePan(formData.pan),
      gstin: normalizeGstin(formData.gstin),
      bankIfscCode: normalizeIfsc(formData.bankIfscCode),
    };

    setIsSubmitting(true);
    try {
      if (editingLandlord) {
        await landlordApi.updateLandlord(editingLandlord.id, payload);
      } else {
        await landlordApi.createLandlord(payload);
      }
      setIsModalOpen(false);
      await loadLandlords();
    } catch (err) {
      alert(getErrorMessage(err, 'Failed to save landlord'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!landlordToDelete) return;
    setIsDeleting(true);
    try {
      await landlordApi.deleteLandlord(landlordToDelete.id);
      setLandlordToDelete(null);
      await loadLandlords();
    } catch (err) {
      alert(getErrorMessage(err, 'Failed to deactivate landlord'));
    } finally {
      setIsDeleting(false);
    }
  };

  if (!brokerMode) {
    return (
      <div className="bg-amber-50 border border-amber-200 rounded-2xl p-8 text-center space-y-3">
        <AlertCircle className="w-10 h-10 text-amber-600 mx-auto" />
        <h2 className="font-bold text-amber-900 text-lg">Landlord Directory Disabled</h2>
        <p className="text-xs text-amber-700 max-w-md mx-auto">
          Your organization is configured in <strong>Direct OWNER</strong> mode. You manage your own properties directly without external property owners. Switch to BROKERAGE or HYBRID mode in Settings if you manage properties for third-party landlords.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Property Owners / Landlords</h1>
          <p className="text-xs text-slate-500 mt-1">
            Directory of external landlords whose properties you manage under brokerage & fee agreements.
          </p>
        </div>
        <Button onClick={handleOpenAdd}>
          <Plus className="w-4 h-4 mr-2" /> Add Landlord
        </Button>
      </div>

      {isLoading ? (
        <TableSkeleton rows={4} columns={3} />
      ) : landlords.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-slate-300 space-y-3">
          <Briefcase className="w-12 h-12 text-slate-400 mx-auto" />
          <h3 className="font-bold text-slate-900">No landlords onboarded</h3>
          <p className="text-xs text-slate-500">Add property owners to assign them to managed properties and process payouts.</p>
          <Button onClick={handleOpenAdd}>
            <Plus className="w-4 h-4 mr-2" /> Add Landlord
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {landlords.map((landlord) => (
            <div
              key={landlord.id}
              className="bg-white rounded-2xl border border-slate-200 shadow-xs hover:shadow-md transition-all p-6 space-y-4 flex flex-col justify-between"
            >
              <div className="space-y-3">
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="font-bold text-base text-slate-900">{landlord.legalName}</h3>
                    <p className="text-[11px] text-slate-400 flex items-center gap-1 mt-0.5">
                      <Building className="w-3 h-3 text-slate-400" /> {landlord.ownerType}
                    </p>
                  </div>
                  <Badge variant={landlord.isActive !== false ? 'success' : 'default'}>
                    {landlord.isActive !== false ? 'Active' : 'Inactive'}
                  </Badge>
                </div>

                <div className="space-y-1 text-xs text-slate-600">
                  {landlord.phone && (
                    <div className="flex items-center gap-2">
                      <Phone className="w-3.5 h-3.5 text-slate-400" />
                      <span>{landlord.phone}</span>
                    </div>
                  )}
                  {landlord.email && (
                    <div className="flex items-center gap-2">
                      <Mail className="w-3.5 h-3.5 text-slate-400" />
                      <span>{landlord.email}</span>
                    </div>
                  )}
                </div>

                {(landlord.pan || landlord.gstin) && (
                  <div className="pt-2 border-t border-slate-100 flex gap-4 text-[11px] text-slate-500">
                    {landlord.pan && <span>PAN: <strong>{landlord.pan}</strong></span>}
                    {landlord.gstin && <span>GST: <strong>{landlord.gstin}</strong></span>}
                  </div>
                )}

                {landlord.bankAccountNumber && (
                  <div className="p-2.5 bg-slate-50 rounded-xl border border-slate-100 text-[11px] text-slate-600 space-y-0.5">
                    <p className="font-semibold text-slate-800">Bank Details</p>
                    <p>A/C: {landlord.bankAccountNumber} ({landlord.bankName || 'Bank'})</p>
                    {landlord.bankIfscCode && <p>IFSC: {landlord.bankIfscCode}</p>}
                  </div>
                )}
              </div>

              <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-2">
                <Button size="sm" variant="secondary" onClick={() => handleOpenEdit(landlord)}>
                  <Edit className="w-3.5 h-3.5 mr-1" /> Edit
                </Button>
                <Button size="sm" variant="danger" onClick={() => setLandlordToDelete({ id: landlord.id, name: landlord.legalName })}>
                  <Trash2 className="w-3.5 h-3.5" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Delete / Deactivate Landlord Confirm Modal */}
      <ConfirmModal
        isOpen={!!landlordToDelete}
        onClose={() => setLandlordToDelete(null)}
        onConfirm={handleConfirmDelete}
        title="Deactivate Landlord"
        description={`Are you sure you want to deactivate landlord "${landlordToDelete?.name || ''}"?`}
        warning="Deactivating a landlord will pause payout processing for their assigned properties."
        confirmText="Deactivate Landlord"
        isLoading={isDeleting}
      />

      {/* Add / Edit Landlord Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingLandlord ? 'Edit Landlord' : 'Add Property Owner'}
        description="Onboard an external property owner to assign properties and track payouts."
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Legal Name *"
              placeholder="e.g. Rajesh Kumar"
              value={formData.legalName}
              onChange={(e) => setFormData({ ...formData, legalName: e.target.value })}
              required
            />
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-slate-700">Owner Entity Type</label>
              <select
                value={formData.ownerType}
                onChange={(e) => setFormData({ ...formData, ownerType: e.target.value as OwnerType })}
                className="w-full h-10 px-3 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="INDIVIDUAL">Individual</option>
                <option value="COMPANY">Company / Corporate</option>
                <option value="TRUST">Trust / Society</option>
                <option value="PARTNERSHIP">Partnership Firm</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Phone Number *"
              placeholder="+91 98765 43210"
              value={formData.phone}
              onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              required
            />
            <Input
              label="Email Address"
              type="email"
              placeholder="rajesh@example.com"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="PAN Number"
              placeholder="ABCDE1234F"
              value={formData.pan}
              onChange={(e) => setFormData({ ...formData, pan: e.target.value })}
            />
            <Input
              label="GSTIN (Optional)"
              placeholder="07AAAAA0000A1Z5"
              value={formData.gstin}
              onChange={(e) => setFormData({ ...formData, gstin: e.target.value })}
            />
          </div>

          <Input
            label="Registered Address"
            placeholder="House/Flat #, Street, City, State"
            value={formData.address}
            onChange={(e) => setFormData({ ...formData, address: e.target.value })}
          />

          <div className="pt-2 border-t border-slate-100">
            <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider mb-2">Payout Bank Details</h4>
            <div className="grid grid-cols-2 gap-3">
              <Input
                label="Account Holder Name"
                placeholder="Name on bank account"
                value={formData.accountHolderName}
                onChange={(e) => setFormData({ ...formData, accountHolderName: e.target.value })}
              />
              <Input
                label="Bank Account Number"
                placeholder="1234567890"
                value={formData.bankAccountNumber}
                onChange={(e) => setFormData({ ...formData, bankAccountNumber: e.target.value })}
              />
              <Input
                label="IFSC Code"
                placeholder="SBIN0001234"
                value={formData.bankIfscCode}
                onChange={(e) => setFormData({ ...formData, bankIfscCode: e.target.value })}
              />
              <Input
                label="Bank Name"
                placeholder="State Bank of India"
                value={formData.bankName}
                onChange={(e) => setFormData({ ...formData, bankName: e.target.value })}
              />
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              {editingLandlord ? 'Save Changes' : 'Onboard Landlord'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
