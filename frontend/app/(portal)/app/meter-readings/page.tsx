'use client';

import React, { useEffect, useState } from 'react';
import {
  Zap,
  Plus,
  Droplets,
  Flame,
  Camera,
  CheckCircle2,
  XCircle,
  AlertCircle,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import {
  meterReadingApi,
  MeterReading,
  MeterType,
  MeterScope,
  VerificationStatus,
} from '@/lib/api/meterReadingApi';
import { propertyApi, Property } from '@/lib/api/propertyApi';
import { unitApi, Unit } from '@/lib/api/unitApi';
import { leaseApi, Lease } from '@/lib/api/leaseApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { FileUpload } from '@/components/ui/FileUpload';
import { TableSkeleton } from '@/components/ui/TableSkeleton';
import { resolveFileUrl } from '@/lib/api/fileApi';
import { getErrorMessage } from '@/lib/utils';
import { useToast } from '@/lib/toast/ToastContext';

export default function MeterReadingsPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const isTenant = user?.role === 'TENANT';

  const [readings, setReadings] = useState<MeterReading[]>([]);
  const [properties, setProperties] = useState<Property[]>([]);
  const [units, setUnits] = useState<Unit[]>([]);
  const [tenantLease, setTenantLease] = useState<Lease | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Log reading modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Dispute modal
  const [disputeModalOpen, setDisputeModalOpen] = useState(false);
  const [disputeReadingId, setDisputeReadingId] = useState<string | null>(null);
  const [disputeReason, setDisputeReason] = useState('');
  const [isDisputing, setIsDisputing] = useState(false);

  // Photo preview modal
  const [previewPhoto, setPreviewPhoto] = useState<string | null>(null);

  // Split Engine modal
  const [isSplitModalOpen, setIsSplitModalOpen] = useState(false);
  const [splitPropertyId, setSplitPropertyId] = useState('');
  const [splitBillingMonth, setSplitBillingMonth] = useState(
    new Date().toISOString().substring(0, 7) + '-01'
  );
  const [isSplitting, setIsSplitting] = useState(false);

  const [formData, setFormData] = useState({
    propertyId: '',
    unitId: '',
    meterType: 'ELECTRICITY' as MeterType,
    meterScope: (isTenant ? 'ROOM_AC' : 'ROOM_AC') as MeterScope,
    previousReading: 0,
    currentReading: 0,
    readingDate: new Date().toISOString().split('T')[0],
    ratePerUnit: 10,
    photoUrl: '',
    notes: '',
  });

  const loadData = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      if (isTenant) {
        const lease = await leaseApi.getMyLease().catch(() => null);
        setTenantLease(lease);
        if (lease?.id) {
          const tenantReadings = await meterReadingApi.getMeterReadingsByLease(lease.id);
          setReadings(tenantReadings || []);
          if (lease.unitId) {
            setFormData((prev) => ({
              ...prev,
              unitId: lease.unitId,
              meterScope: 'ROOM_AC',
            }));
          }
        } else {
          setReadings([]);
        }
      } else {
        const [readingsRes, propertiesRes] = await Promise.all([
          meterReadingApi.getMeterReadings(),
          propertyApi.getProperties(),
        ]);
        setReadings(readingsRes || []);
        setProperties(propertiesRes || []);
        if (propertiesRes && propertiesRes.length > 0) {
          setSplitPropertyId(propertiesRes[0].id);
        }
        const unitsArrays = await Promise.all(
          (propertiesRes || []).map((p) => unitApi.getUnitsByProperty(p.id))
        );
        setUnits(unitsArrays.flat() || []);
      }
    } catch (err) {
      console.error('Failed to load meter reading data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user?.organizationId, user?.role]);

  const handleOpenCreateModal = () => {
    setFormData({
      propertyId: properties[0]?.id || '',
      unitId: tenantLease?.unitId || units[0]?.id || '',
      meterType: 'ELECTRICITY',
      meterScope: isTenant ? 'ROOM_AC' : 'ROOM_AC',
      previousReading: 0,
      currentReading: 0,
      readingDate: new Date().toISOString().split('T')[0],
      ratePerUnit: 10,
      photoUrl: '',
      notes: '',
    });
    setIsModalOpen(true);
  };

  const handleCreateReading = async (e: React.FormEvent) => {
    e.preventDefault();
    if (formData.currentReading < formData.previousReading) {
      toast.warning('Current reading cannot be less than previous reading.');
      return;
    }
    if (!formData.photoUrl && (formData.meterScope === 'ROOM_AC' || formData.meterScope === 'MASTER_DISCOM')) {
      const confirmNoPhoto = window.confirm(
        'Upload of meter photo proof is recommended to avoid billing disputes. Do you want to proceed without photo?'
      );
      if (!confirmNoPhoto) return;
    }

    setIsSubmitting(true);
    try {
      await meterReadingApi.createMeterReading({
        propertyId: formData.propertyId || undefined,
        unitId: formData.unitId || undefined,
        meterType: formData.meterType,
        meterScope: formData.meterScope,
        previousReading: formData.previousReading,
        currentReading: formData.currentReading,
        ratePerUnit: formData.ratePerUnit,
        readingDate: formData.readingDate,
        photoUrl: formData.photoUrl || undefined,
        notes: formData.notes || undefined,
      });
      toast.success('Meter reading logged successfully!');
      setIsModalOpen(false);
      await loadData();
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to log meter reading'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleVerify = async (id: string, status: VerificationStatus, reason?: string) => {
    try {
      await meterReadingApi.verifyMeterReading(id, status, reason);
      await loadData();
      if (status === 'VERIFIED') {
        toast.success('Meter reading confirmed and verified successfully!');
      } else {
        toast.warning('Dispute recorded. Management and roommates will be notified.');
      }
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to update reading status'));
    }
  };

  const handleSubmitDispute = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!disputeReadingId || !disputeReason.trim()) return;
    setIsDisputing(true);
    try {
      await handleVerify(disputeReadingId, 'DISPUTED', disputeReason.trim());
      setDisputeModalOpen(false);
      setDisputeReason('');
      setDisputeReadingId(null);
    } finally {
      setIsDisputing(false);
    }
  };

  const handleTriggerSplit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!splitPropertyId) {
      toast.warning('Please select a property.');
      return;
    }
    setIsSplitting(true);
    try {
      const invoices = await meterReadingApi.runUtilitySplit(splitPropertyId, splitBillingMonth);
      setIsSplitModalOpen(false);
      toast.success(
        `Utility split completed! Generated ${invoices.length} decoupled utility invoices.`
      );
      await loadData();
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to run utility split'));
    } finally {
      setIsSplitting(false);
    }
  };

  const getMeterIcon = (type: MeterType) => {
    switch (type) {
      case 'WATER':
        return <Droplets className="w-4 h-4 text-cyan-600" />;
      case 'GAS':
        return <Flame className="w-4 h-4 text-amber-600" />;
      default:
        return <Zap className="w-4 h-4 text-yellow-600" />;
    }
  };

  const getScopeBadge = (scope?: MeterScope) => {
    switch (scope) {
      case 'MASTER_DISCOM':
        return <Badge variant="purple">Master Discom</Badge>;
      case 'ROOM_AC':
        return <Badge variant="warning">Room AC</Badge>;
      case 'HALL_AC':
        return <Badge variant="info">Hall AC</Badge>;
      case 'COMMON_AREA':
        return <Badge variant="success">Common Area</Badge>;
      default:
        return <Badge variant="default">General</Badge>;
    }
  };

  const getVerificationBadge = (status?: VerificationStatus) => {
    switch (status) {
      case 'VERIFIED':
      case 'APPROVED':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-100/80 px-2 py-0.5 rounded-full">
            <CheckCircle2 className="w-3 h-3" /> Verified
          </span>
        );
      case 'DISPUTED':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-rose-700 bg-rose-100/80 px-2 py-0.5 rounded-full">
            <XCircle className="w-3 h-3" /> Disputed
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-amber-700 bg-amber-100/80 px-2 py-0.5 rounded-full">
            <AlertCircle className="w-3 h-3" /> Pending Review
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            {isTenant ? 'AC & Utility Meter Readings' : 'Utility & AC Sub-Meter Readings'}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {isTenant
              ? 'Upload your room AC sub-meter reading with photo proof and review roommate submissions (Solution A dual-party verification).'
              : 'Log monthly Discom bills & AC sub-meter readings with photo audit trails and run automated multi-tier PG electricity splits.'}
          </p>
        </div>

        <div className="flex items-center gap-3">
          {!isTenant && (
            <Button
              variant="outline"
              onClick={() => setIsSplitModalOpen(true)}
              className="border-purple-300 text-purple-700 hover:bg-purple-50"
            >
              <Zap className="w-4 h-4 mr-2 text-purple-600" /> Run Electricity Split
            </Button>
          )}

          <Button onClick={handleOpenCreateModal}>
            <Plus className="w-4 h-4 mr-2" />
            {isTenant ? 'Submit AC Reading' : 'Log Meter Reading'}
          </Button>
        </div>
      </div>

      {isLoading ? (
        <TableSkeleton rows={5} columns={7} />
      ) : readings.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-slate-300 space-y-3">
          <Zap className="w-12 h-12 text-slate-400 mx-auto" />
          <h3 className="font-bold text-slate-900">
            {isTenant ? 'No meter readings logged yet' : 'No meter readings logged'}
          </h3>
          <p className="text-xs text-slate-500 max-w-md mx-auto">
            {isTenant
              ? 'Submit your room AC sub-meter reading at the end of the month with a clear photo proof so that AC units are split fairly among roommates.'
              : 'Record master Discom and room AC sub-meter units to trigger transparent decoupled utility billing.'}
          </p>
          <Button onClick={handleOpenCreateModal}>
            <Plus className="w-4 h-4 mr-2" />
            {isTenant ? 'Submit Room AC Reading' : 'Log Meter Reading'}
          </Button>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <caption className="sr-only">Utility meter readings and verification status</caption>
              <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-semibold">
                <tr>
                  <th className="px-6 py-3.5">Reading Date</th>
                  <th className="px-6 py-3.5">Scope & Utility</th>
                  <th className="px-6 py-3.5">Unit / Room</th>
                  <th className="px-6 py-3.5">Readings (Prev → Curr)</th>
                  <th className="px-6 py-3.5">Consumed</th>
                  <th className="px-6 py-3.5">Photo Proof</th>
                  <th className="px-6 py-3.5">Verification</th>
                  <th className="px-6 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {readings.map((r) => {
                  const consumed = Math.max(0, (r.currentReading || 0) - (r.previousReading || 0));
                  return (
                    <tr key={r.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="px-6 py-4 font-bold text-slate-900">{r.readingDate}</td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-1.5 flex-wrap">
                          {getMeterIcon(r.meterType)}
                          {getScopeBadge(r.meterScope)}
                        </div>
                      </td>
                      <td className="px-6 py-4 text-slate-800 font-semibold">
                        {r.unitNumber ? `Unit ${r.unitNumber}` : 'Master / Common'}
                      </td>
                      <td className="px-6 py-4 text-slate-600">
                        {r.previousReading} → <span className="font-bold text-slate-900">{r.currentReading}</span>
                      </td>
                      <td className="px-6 py-4 font-bold text-blue-600">{consumed} units</td>
                      <td className="px-6 py-4">
                        {r.photoUrl ? (
                          <button
                            type="button"
                            onClick={() => setPreviewPhoto(resolveFileUrl(r.photoUrl!))}
                            className="inline-flex items-center gap-1.5 text-blue-700 hover:text-blue-800 bg-blue-50 hover:bg-blue-100 px-2 py-1 rounded-lg border border-blue-200 text-[11px] font-semibold transition-colors"
                          >
                            <Camera className="w-3.5 h-3.5" /> View Photo
                          </button>
                        ) : (
                          <span className="text-slate-400 italic text-[11px]">No photo</span>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        <div className="space-y-1">
                          {getVerificationBadge(r.verificationStatus)}
                          {r.disputeReason && (
                            <p className="text-[10px] text-rose-600 font-normal">
                              Note: {r.disputeReason}
                            </p>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4 text-right">
                        {(r.verificationStatus === 'PENDING' || r.verificationStatus === 'PENDING_VERIFICATION') && (
                          <div className="flex items-center justify-end gap-1.5">
                            <button
                              type="button"
                              onClick={() => handleVerify(r.id, 'VERIFIED')}
                              title="Confirm / Verify Reading"
                              className="px-2 py-1 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-300 rounded text-[11px] font-bold flex items-center gap-1"
                            >
                              <CheckCircle2 className="w-3 h-3" /> Confirm
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                setDisputeReadingId(r.id);
                                setDisputeModalOpen(true);
                              }}
                              title="Dispute Reading"
                              className="px-2 py-1 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-300 rounded text-[11px] font-bold flex items-center gap-1"
                            >
                              <XCircle className="w-3 h-3" /> Dispute
                            </button>
                          </div>
                        )}
                        {(r.verificationStatus === 'VERIFIED' || r.verificationStatus === 'APPROVED') && (
                          <span className="text-emerald-600 text-[11px] font-semibold">Locked</span>
                        )}
                        {r.verificationStatus === 'DISPUTED' && !isTenant && (
                          <button
                            type="button"
                            onClick={() => handleVerify(r.id, 'VERIFIED')}
                            className="px-2 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300 rounded text-[10px]"
                          >
                            Resolve & Confirm
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Log / Submit Meter Reading Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={isTenant ? 'Submit Room AC Meter Reading' : 'Log Utility / AC Meter Reading'}
        description={
          isTenant
            ? 'Enter your current room AC meter reading and attach a clear photo for roommate verification.'
            : 'Record meter reading and upload photo proof for multi-tier PG split calculation.'
        }
      >
        <form onSubmit={handleCreateReading} className="space-y-4">
          {!isTenant && (
            <div className="grid grid-cols-2 gap-3">
              <div className="w-full flex flex-col gap-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                  Property *
                </label>
                <select
                  className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={formData.propertyId}
                  onChange={(e) => setFormData({ ...formData, propertyId: e.target.value })}
                  required
                >
                  <option value="">Select Property...</option>
                  {properties.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.type})
                    </option>
                  ))}
                </select>
              </div>

              <div className="w-full flex flex-col gap-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                  Meter Scope *
                </label>
                <select
                  className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={formData.meterScope}
                  onChange={(e) =>
                    setFormData({ ...formData, meterScope: e.target.value as MeterScope })
                  }
                  required
                >
                  <option value="MASTER_DISCOM">Master Discom Bill (Entire Property)</option>
                  <option value="ROOM_AC">Room AC Sub-Meter (Split across roommates)</option>
                  <option value="HALL_AC">Hall AC Sub-Meter (Split across all PG)</option>
                  <option value="COMMON_AREA">Common Area Meter</option>
                </select>
              </div>
            </div>
          )}

          {formData.meterScope !== 'MASTER_DISCOM' && !isTenant && (
            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                Select Unit / Room
              </label>
              <select
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={formData.unitId}
                onChange={(e) => setFormData({ ...formData, unitId: e.target.value })}
              >
                <option value="">Select unit/room...</option>
                {units.map((u) => (
                  <option key={u.id} value={u.id}>
                    Unit {u.unitNumber} ({u.status})
                  </option>
                ))}
              </select>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                Utility Type
              </label>
              <select
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={formData.meterType}
                onChange={(e) =>
                  setFormData({ ...formData, meterType: e.target.value as MeterType })
                }
              >
                <option value="ELECTRICITY">Electricity</option>
                <option value="WATER">Water</option>
                <option value="GAS">Gas</option>
              </select>
            </div>
            <Input
              label="Reading Date"
              type="date"
              value={formData.readingDate}
              onChange={(e) => setFormData({ ...formData, readingDate: e.target.value })}
              required
            />
          </div>

          <div className="grid grid-cols-3 gap-3">
            <Input
              label="Previous Reading"
              type="number"
              value={formData.previousReading}
              onChange={(e) =>
                setFormData({ ...formData, previousReading: parseFloat(e.target.value) || 0 })
              }
              required
            />
            <Input
              label="Current Reading"
              type="number"
              value={formData.currentReading}
              onChange={(e) =>
                setFormData({ ...formData, currentReading: parseFloat(e.target.value) || 0 })
              }
              required
            />
            <Input
              label="Rate per Unit (₹)"
              type="number"
              step="0.01"
              value={formData.ratePerUnit}
              onChange={(e) =>
                setFormData({ ...formData, ratePerUnit: parseFloat(e.target.value) || 0 })
              }
              required
            />
          </div>

          {/* Photo Proof Upload */}
          <FileUpload
            label="Meter Photo Proof (Recommended)"
            category="PROPERTY_IMAGE"
            value={formData.photoUrl}
            onChange={(url) => setFormData({ ...formData, photoUrl: url })}
            placeholder="Upload or take a photo of the meter dial"
          />

          <Input
            label="Notes / Audit Description"
            placeholder="e.g. End of month reading taken with roommate present"
            value={formData.notes}
            onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
          />

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              {isTenant ? 'Submit AC Reading' : 'Save Meter Reading'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Dispute Modal */}
      <Modal
        isOpen={disputeModalOpen}
        onClose={() => setDisputeModalOpen(false)}
        title="Dispute Meter Reading"
        description="Explain why you disagree with this reading. Management and roommates will review it."
      >
        <form onSubmit={handleSubmitDispute} className="space-y-4">
          <div className="w-full flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Dispute Reason *
            </label>
            <textarea
              rows={3}
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              placeholder="e.g. The photo shows 340 units, but 430 was typed in; or reading was taken after I vacated."
              value={disputeReason}
              onChange={(e) => setDisputeReason(e.target.value)}
              required
            />
          </div>
          <div className="flex items-center justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setDisputeModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="danger" isLoading={isDisputing}>
              Submit Dispute
            </Button>
          </div>
        </form>
      </Modal>

      {/* Run Split Calculation Modal */}
      <Modal
        isOpen={isSplitModalOpen}
        onClose={() => setIsSplitModalOpen(false)}
        title="Run PG Electricity Split Engine"
        description="Calculate common area deduction and room AC shares, and issue independent UTILITY_ONLY invoices."
      >
        <form onSubmit={handleTriggerSplit} className="space-y-4">
          <div className="w-full flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Select PG Property *
            </label>
            <select
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
              value={splitPropertyId}
              onChange={(e) => setSplitPropertyId(e.target.value)}
              required
            >
              <option value="">Select Property...</option>
              {properties.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.city})
                </option>
              ))}
            </select>
          </div>

          <Input
            label="Billing Month (1st day of month)"
            type="date"
            value={splitBillingMonth}
            onChange={(e) => setSplitBillingMonth(e.target.value)}
            required
          />

          <div className="bg-purple-50 p-3 rounded-xl border border-purple-200 text-xs text-purple-900 space-y-1">
            <p className="font-bold flex items-center gap-1.5">
              <Zap className="w-4 h-4 text-purple-600" /> Decoupled Utility Billing:
            </p>
            <p className="text-[11px] text-purple-700">
              Generates standalone <span className="font-semibold">UTILITY_ONLY</span> invoices
              separate from monthly rent receipts. Invoices will carry complete transparent audit
              metadata with photo links visible in tenant portals.
            </p>
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setIsSplitModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSplitting} className="bg-purple-700 hover:bg-purple-800">
              Calculate & Issue Invoices
            </Button>
          </div>
        </form>
      </Modal>

      {/* Photo Preview Modal */}
      {previewPhoto && (
        <Modal
          isOpen={true}
          onClose={() => setPreviewPhoto(null)}
          title="Meter Reading Photo Proof"
          description="High-resolution proof photo captured during the audit cycle."
        >
          <div className="space-y-4">
            <div className="bg-slate-900 rounded-xl overflow-hidden flex items-center justify-center max-h-[450px]">
              <img
                src={previewPhoto}
                alt="Meter Reading Proof"
                className="max-h-[440px] max-w-full object-contain"
              />
            </div>
            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => setPreviewPhoto(null)}
                className="px-4 py-2 text-xs font-semibold bg-slate-100 hover:bg-slate-200 rounded-lg text-slate-700"
              >
                Close Preview
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
