'use client';

import React, { useEffect, useState } from 'react';
import { User, FileText, Receipt as ReceiptIcon, Download, Wrench, ShieldCheck, CheckCircle2 } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { tenantApi, Tenant } from '@/lib/api/tenantApi';
import { leaseApi, Lease } from '@/lib/api/leaseApi';
import { receiptApi, Receipt } from '@/lib/api/receiptApi';
import { ticketApi, TicketCreatePayload } from '@/lib/api/ticketApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { formatCurrency, getErrorMessage } from '@/lib/utils';

export default function TenantSelfPortalPage() {
  const { user } = useAuth();
  const [profile, setProfile] = useState<Tenant | null>(null);
  const [lease, setLease] = useState<Lease | null>(null);
  const [receipts, setReceipts] = useState<Receipt[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [isTicketModalOpen, setIsTicketModalOpen] = useState(false);
  const [isSubmittingTicket, setIsSubmittingTicket] = useState(false);
  const [ticketForm, setTicketForm] = useState<TicketCreatePayload>({
    unitId: '',
    tenantId: '',
    title: '',
    description: '',
    category: 'PLUMBING',
    priority: 'MEDIUM',
  });

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [profileRes, leaseRes, receiptsRes] = await Promise.all([
        tenantApi.getMyProfile().catch(() => null),
        leaseApi.getMyLease().catch(() => null),
        receiptApi.getMyReceipts().catch(() => []),
      ]);
      setProfile(profileRes);
      setLease(leaseRes);
      setReceipts(receiptsRes || []);
    } catch (err) {
      console.error('Failed to load tenant portal data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleDownloadAgreementPdf = async () => {
    if (!lease?.id) return;
    try {
      const blob = await leaseApi.downloadPdf(lease.id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Rent_Agreement_${lease.id}.pdf`;
      a.click();
    } catch (err) {
      alert('Failed to download rent agreement PDF');
    }
  };

  const handleDownloadReceiptPdf = async (receiptId: string) => {
    try {
      const blob = await receiptApi.downloadPdf(receiptId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Receipt_${receiptId}.pdf`;
      a.click();
    } catch (err) {
      alert('Failed to download receipt PDF');
    }
  };

  const handleOpenTicketModal = () => {
    if (!lease || !profile) return;
    setTicketForm({
      unitId: lease.unitId,
      tenantId: profile.id,
      title: '',
      description: '',
      category: 'PLUMBING',
      priority: 'MEDIUM',
    });
    setIsTicketModalOpen(true);
  };

  const handleCreateTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmittingTicket(true);
    try {
      await ticketApi.createTicket(ticketForm);
      alert('Maintenance ticket submitted successfully!');
      setIsTicketModalOpen(false);
    } catch (err) {
      alert(getErrorMessage(err, 'Failed to submit maintenance ticket'));
    } finally {
      setIsSubmittingTicket(false);
    }
  };

  if (isLoading) {
    return <div className="text-center py-12 text-slate-400 text-sm">Loading resident portal...</div>;
  }

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="bg-gradient-to-r from-blue-900 to-slate-900 text-white p-8 rounded-3xl shadow-lg flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
            <span className="px-3 py-1 bg-blue-500/20 text-blue-300 font-bold text-xs uppercase tracking-wider rounded-full border border-blue-400/20">
              Resident Self-Service
            </span>
            {profile?.kycStatus && (
              <Badge variant={profile.kycStatus === 'VERIFIED' ? 'success' : 'warning'}>
                KYC: {profile.kycStatus}
              </Badge>
            )}
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight">Welcome, {user?.fullName || profile?.fullName || 'Resident'}</h1>
          <p className="text-xs text-slate-300">
            Access your active rent agreement, download payment receipts, and submit maintenance requests.
          </p>
        </div>
        {lease && (
          <Button variant="primary" onClick={handleOpenTicketModal}>
            <Wrench className="w-4 h-4 mr-2" /> Raise Maintenance Request
          </Button>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Profile Details Card */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs space-y-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
              <User className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-slate-900">Resident Profile</h3>
              <p className="text-[11px] text-slate-400">KYC & Contact Info</p>
            </div>
          </div>
          <div className="space-y-2 text-xs text-slate-600 pt-2 border-t border-slate-100">
            <p><strong>Name:</strong> {profile?.fullName || user?.fullName}</p>
            <p><strong>Email:</strong> {profile?.email || user?.email}</p>
            <p><strong>Phone:</strong> {profile?.phone || 'N/A'}</p>
            <p><strong>Address:</strong> {profile?.permanentAddress || 'N/A'}</p>
            <p><strong>ID Proof:</strong> {profile?.idProofType || 'Aadhaar'} ({profile?.idProofLast4 || 'Verified'})</p>
          </div>
        </div>

        {/* Active Agreement Card */}
        <div className="md:col-span-2 bg-white rounded-2xl p-6 border border-slate-200 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
                <FileText className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-bold text-slate-900">Active Rent Agreement</h3>
                <p className="text-[11px] text-slate-400">Digital Tenancy Record</p>
              </div>
            </div>
            {lease && (
              <Button size="sm" variant="secondary" onClick={handleDownloadAgreementPdf}>
                <Download className="w-3.5 h-3.5 mr-1" /> PDF Agreement
              </Button>
            )}
          </div>

          {!lease ? (
            <p className="text-xs text-slate-500 py-4">No active lease agreement found for your profile.</p>
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs pt-2 border-t border-slate-100">
              <div>
                <span className="text-[11px] text-slate-400 font-semibold uppercase">Tenancy Period</span>
                <p className="font-bold text-slate-900 mt-0.5">{lease.startDate} to {lease.endDate}</p>
              </div>
              <div>
                <span className="text-[11px] text-slate-400 font-semibold uppercase">Monthly Rent</span>
                <p className="font-bold text-emerald-600 mt-0.5">{formatCurrency(lease.monthlyRent)}</p>
              </div>
              <div>
                <span className="text-[11px] text-slate-400 font-semibold uppercase">Rent Due Day</span>
                <p className="font-bold text-slate-900 mt-0.5">Day {lease.rentDueDay} of month</p>
              </div>
              <div>
                <span className="text-[11px] text-slate-400 font-semibold uppercase">Notice Period</span>
                <p className="font-bold text-slate-900 mt-0.5">{lease.noticePeriodDays} Days</p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Receipts History */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 space-y-4 shadow-xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center font-bold">
              <ReceiptIcon className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-slate-900">Your Payment Receipts</h3>
              <p className="text-[11px] text-slate-400">Rent & Utility receipts issued by management</p>
            </div>
          </div>
        </div>

        {receipts.length === 0 ? (
          <p className="text-xs text-slate-500 py-4 text-center">No payment receipts issued yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-semibold">
                <tr>
                  <th className="px-4 py-3">Receipt #</th>
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3 text-right">Download</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {receipts.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3 font-bold text-slate-900">{r.receiptNumber}</td>
                    <td className="px-4 py-3 text-slate-600">{r.paymentDate}</td>
                    <td className="px-4 py-3"><Badge variant="purple">{r.receiptType}</Badge></td>
                    <td className="px-4 py-3 font-bold text-slate-900">{formatCurrency(r.amount)}</td>
                    <td className="px-4 py-3 text-right">
                      <Button size="sm" variant="secondary" onClick={() => handleDownloadReceiptPdf(r.id)}>
                        <Download className="w-3.5 h-3.5 mr-1" /> PDF
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Ticket Modal */}
      <Modal
        isOpen={isTicketModalOpen}
        onClose={() => setIsTicketModalOpen(false)}
        title="Raise Maintenance Ticket"
        description="Submit a request for plumbing, electrical or appliance repair."
      >
        <form onSubmit={handleCreateTicket} className="space-y-4">
          <Input
            label="Issue Title *"
            placeholder="e.g. Tap leaking in master bathroom"
            value={ticketForm.title}
            onChange={(e) => setTicketForm({ ...ticketForm, title: e.target.value })}
            required
          />

          <div className="grid grid-cols-2 gap-3">
            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">Category</label>
              <select
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={ticketForm.category}
                onChange={(e) => setTicketForm({ ...ticketForm, category: e.target.value as any })}
              >
                <option value="PLUMBING">Plumbing</option>
                <option value="ELECTRICAL">Electrical</option>
                <option value="APPLIANCE">Appliance Repair</option>
                <option value="CARPENTRY">Carpentry</option>
                <option value="CLEANING">Cleaning</option>
                <option value="PEST_CONTROL">Pest Control</option>
                <option value="INTERNET">Internet / Wi-Fi</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">Priority</label>
              <select
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={ticketForm.priority}
                onChange={(e) => setTicketForm({ ...ticketForm, priority: e.target.value as any })}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>
          </div>

          <div className="w-full flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">Description</label>
            <textarea
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 placeholder:text-slate-400"
              rows={3}
              placeholder="Describe the issue and preferred inspection time..."
              value={ticketForm.description}
              onChange={(e) => setTicketForm({ ...ticketForm, description: e.target.value })}
            />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setIsTicketModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmittingTicket}>
              Submit Request
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
