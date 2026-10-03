'use client';

import React, { useEffect, useState } from 'react';
import { FileText, Plus, Download, CheckCircle, Clock, XCircle, AlertCircle } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { leaseApi, Lease, LeaseStatus } from '@/lib/api/leaseApi';
import { propertyApi, Property } from '@/lib/api/propertyApi';
import { unitApi, Unit } from '@/lib/api/unitApi';
import { tenantApi, Tenant } from '@/lib/api/tenantApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { ConfirmModal } from '@/components/ui/ConfirmModal';
import { TableSkeleton } from '@/components/ui/TableSkeleton';
import { Input } from '@/components/ui/Input';
import { formatCurrency, getErrorMessage } from '@/lib/utils';

export default function LeasesPage() {
  const { user } = useAuth();
  const [leases, setLeases] = useState<Lease[]>([]);
  const [properties, setProperties] = useState<Property[]>([]);
  const [units, setUnits] = useState<Unit[]>([]);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [selectedPropertyId, setSelectedPropertyId] = useState<string>('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    unitId: '',
    tenantId: '',
    startDate: new Date().toISOString().split('T')[0],
    endDate: new Date(Date.now() + 365 * 86400000).toISOString().split('T')[0],
    monthlyRent: 15000,
    securityDeposit: 30000,
    customClauses: 'Standard 11-Month Rental Agreement with 1 Month Notice Period.',
    termsAndConditions: '',
  });

  const loadData = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      const [leasesRes, propsRes, tenantsRes] = await Promise.all([
        leaseApi.getLeases(),
        propertyApi.getProperties(),
        tenantApi.getTenants(),
      ]);
      setLeases(leasesRes || []);
      setProperties(propsRes || []);
      setTenants(tenantsRes || []);

      if (propsRes && propsRes.length > 0) {
        setSelectedPropertyId(propsRes[0].id);
        const unitList = await unitApi.getUnitsByProperty(propsRes[0].id);
        setUnits(unitList.filter((u) => u.status === 'AVAILABLE') || []);
      }
    } catch (err) {
      console.error('Failed to load lease data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user?.organizationId]);

  const handlePropertyChange = async (propertyId: string) => {
    setSelectedPropertyId(propertyId);
    if (!user?.organizationId) return;
    const unitList = await unitApi.getUnitsByProperty(propertyId);
    setUnits(unitList.filter((u) => u.status === 'AVAILABLE') || []);
  };

  const handleCreateLease = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.organizationId) return;
    setIsSubmitting(true);
    try {
      await leaseApi.createLease({
        status: 'DRAFT',
        ...formData,
      });
      setIsModalOpen(false);
      await loadData();
    } catch (err: unknown) {
      alert(getErrorMessage(err, 'Failed to create lease. Check overlapping dates or unit availability.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleStatusTransition = async (leaseId: string, targetStatus: LeaseStatus) => {
    if (!user?.organizationId) return;
    try {
      await leaseApi.updateLeaseStatus(leaseId, targetStatus);
      await loadData();
    } catch (err: unknown) {
      alert(getErrorMessage(err, 'Invalid state transition.'));
    }
  };

  const handleDownloadPdf = async (leaseId: string) => {
    if (!user?.organizationId) return;
    try {
      const blob = await leaseApi.downloadPdf(leaseId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Rent_Agreement_${leaseId}.pdf`;
      a.click();
    } catch (err) {
      alert('Failed to generate PDF agreement.');
    }
  };

  const getStatusBadge = (status: LeaseStatus) => {
    switch (status) {
      case 'DRAFT':
        return <Badge variant="warning">Draft</Badge>;
      case 'PENDING_ESIGN':
        return <Badge variant="info">Pending E-Sign</Badge>;
      case 'ACTIVE':
        return <Badge variant="success">Active Lease</Badge>;
      case 'EXPIRED':
        return <Badge variant="default">Expired</Badge>;
      case 'TERMINATED':
        return <Badge variant="danger">Terminated</Badge>;
      case 'CANCELLED':
        return <Badge variant="danger">Cancelled</Badge>;
    }
  };

  const [leaseToConfirm, setLeaseToConfirm] = useState<{ id: string; targetStatus: LeaseStatus } | null>(null);
  const [isTransitioning, setIsTransitioning] = useState(false);

  const confirmStatusTransition = async () => {
    if (!leaseToConfirm || !user?.organizationId) return;
    setIsTransitioning(true);
    try {
      await leaseApi.updateLeaseStatus(leaseToConfirm.id, leaseToConfirm.targetStatus);
      setLeaseToConfirm(null);
      await loadData();
    } catch (err: unknown) {
      alert(getErrorMessage(err, 'Invalid state transition.'));
    } finally {
      setIsTransitioning(false);
    }
  };

  const isExpiringSoon = (lease: Lease) => {
    if (lease.status !== 'ACTIVE' || !lease.endDate) return false;
    const end = new Date(lease.endDate).getTime();
    const now = Date.now();
    const diffDays = (end - now) / (1000 * 3600 * 24);
    return diffDays >= 0 && diffDays <= 30;
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Leases & Agreements</h1>
          <p className="text-xs text-slate-500 mt-1">
            Automated lease status lifecycle (Draft → Pending E-Sign → Active → Expired), overlap validation & PDF execution.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>
          <Plus className="w-4 h-4 mr-2" /> Create Rent Agreement
        </Button>
      </div>

      {isLoading ? (
        <TableSkeleton rows={5} columns={6} />
      ) : leases.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-slate-300 space-y-4">
          <FileText className="w-12 h-12 text-slate-400 mx-auto" />
          <div>
            <h3 className="font-bold text-slate-900">No leases created yet</h3>
            <p className="text-xs text-slate-500 mt-1">Create your first lease agreement from available property units.</p>
          </div>
          <Button onClick={() => setIsModalOpen(true)}>
            <Plus className="w-4 h-4 mr-2" /> Create Rent Agreement
          </Button>
        </div>
      ) : (
        <div className="space-y-4">
          {leases.map((lease) => (
            <div
              key={lease.id}
              className="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-6 hover:border-blue-400 transition-all"
            >
              <div className="space-y-2">
                <div className="flex items-center gap-3">
                  <h3 className="font-bold text-slate-900 text-base">Lease #{lease.id.substring(0, 8)}</h3>
                  {getStatusBadge(lease.status)}
                  {isExpiringSoon(lease) && (
                    <Badge variant="warning">Expires Soon ({lease.endDate})</Badge>
                  )}
                </div>

                <div className="flex flex-wrap items-center gap-x-6 gap-y-1 text-xs text-slate-600 font-medium">
                  <span>Start: <strong>{lease.startDate}</strong></span>
                  <span>End: <strong>{lease.endDate}</strong></span>
                  <span>Monthly Rent: <strong>{formatCurrency(lease.monthlyRent)}</strong></span>
                  <span>Deposit: <strong>{formatCurrency(lease.securityDeposit)}</strong></span>
                </div>
              </div>

              <div className="flex items-center gap-2 flex-wrap">
                {/* State Machine Transition Actions */}
                {lease.status === 'DRAFT' && (
                  <>
                    <Button size="sm" variant="outline" onClick={() => handleStatusTransition(lease.id, 'PENDING_ESIGN')}>
                      Send for E-Sign
                    </Button>
                    <Button size="sm" variant="primary" onClick={() => handleStatusTransition(lease.id, 'ACTIVE')}>
                      Activate Lease
                    </Button>
                    <Button size="sm" variant="ghost" className="text-red-600" onClick={() => setLeaseToConfirm({ id: lease.id, targetStatus: 'CANCELLED' })}>
                      Cancel
                    </Button>
                  </>
                )}

                {lease.status === 'PENDING_ESIGN' && (
                  <>
                    <Button size="sm" variant="primary" onClick={() => handleStatusTransition(lease.id, 'ACTIVE')}>
                      Mark E-Signed & Activate
                    </Button>
                    <Button size="sm" variant="ghost" className="text-red-600" onClick={() => setLeaseToConfirm({ id: lease.id, targetStatus: 'CANCELLED' })}>
                      Cancel
                    </Button>
                  </>
                )}

                {lease.status === 'ACTIVE' && (
                  <Button size="sm" variant="outline" className="border-red-200 text-red-700 hover:bg-red-50" onClick={() => setLeaseToConfirm({ id: lease.id, targetStatus: 'TERMINATED' })}>
                    Terminate Lease
                  </Button>
                )}

                <Button size="sm" variant="secondary" onClick={() => handleDownloadPdf(lease.id)}>
                  <Download className="w-3.5 h-3.5 mr-1.5" /> Agreement PDF
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Confirm Lease Termination / Cancellation Modal */}
      <ConfirmModal
        isOpen={!!leaseToConfirm}
        onClose={() => setLeaseToConfirm(null)}
        onConfirm={confirmStatusTransition}
        title={leaseToConfirm?.targetStatus === 'TERMINATED' ? 'Terminate Lease Agreement' : 'Cancel Lease Agreement'}
        description={`Are you sure you want to ${leaseToConfirm?.targetStatus === 'TERMINATED' ? 'terminate' : 'cancel'} Lease #${leaseToConfirm?.id.substring(0, 8) || ''}?`}
        warning="This action will release the assigned unit back to AVAILABLE status for new bookings."
        confirmText={leaseToConfirm?.targetStatus === 'TERMINATED' ? 'Terminate Lease' : 'Cancel Lease'}
        isLoading={isTransitioning}
      />

      {/* Create Lease Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create New Lease Agreement"
        description="Select property, available unit, and tenant"
      >
        <form onSubmit={handleCreateLease} className="space-y-4">
          <div className="w-full flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              1. Select Property
            </label>
            <select
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={selectedPropertyId}
              onChange={(e) => handlePropertyChange(e.target.value)}
            >
              {properties.map((p) => (
                <option key={p.id} value={p.id}>{p.name} ({p.type})</option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                2. Available Unit
              </label>
              <select
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={formData.unitId}
                onChange={(e) => setFormData({ ...formData, unitId: e.target.value })}
                required
              >
                <option value="">Select available unit...</option>
                {units.map((u) => (
                  <option key={u.id} value={u.id}>Unit {u.unitNumber} ({formatCurrency(u.monthlyRent)}/mo)</option>
                ))}
              </select>
            </div>

            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                3. Tenant
              </label>
              <select
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={formData.tenantId}
                onChange={(e) => setFormData({ ...formData, tenantId: e.target.value })}
                required
              >
                <option value="">Select registered tenant...</option>
                {tenants.map((t) => (
                  <option key={t.id} value={t.id}>{t.fullName} ({t.phone})</option>
                ))}
              </select>
            </div>
          </div>

          <div className="w-full flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Terms & Conditions
            </label>
            <textarea
              className="min-h-24 w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Enter agreement terms and conditions"
              value={formData.termsAndConditions}
              onChange={(e) => setFormData({ ...formData, termsAndConditions: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Start Date"
              type="date"
              value={formData.startDate}
              onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
              required
            />
            <Input
              label="End Date"
              type="date"
              value={formData.endDate}
              onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Monthly Rent (₹)"
              type="number"
              value={formData.monthlyRent}
              onChange={(e) => setFormData({ ...formData, monthlyRent: parseFloat(e.target.value) || 0 })}
              required
            />
            <Input
              label="Security Deposit (₹)"
              type="number"
              value={formData.securityDeposit}
              onChange={(e) => setFormData({ ...formData, securityDeposit: parseFloat(e.target.value) || 0 })}
              required
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Generate Draft Agreement
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
