'use client';

import React, { useEffect, useState } from 'react';
import {
  User,
  FileText,
  Receipt as ReceiptIcon,
  Download,
  Wrench,
  FileSpreadsheet,
  CheckCircle2,
  Calendar,
  CreditCard,
  Building,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { useToast } from '@/lib/toast/ToastContext';
import { tenantApi, Tenant } from '@/lib/api/tenantApi';
import { leaseApi, Lease } from '@/lib/api/leaseApi';
import { receiptApi, Receipt } from '@/lib/api/receiptApi';
import { invoiceApi, Invoice } from '@/lib/api/invoiceApi';
import { ticketApi, TicketCreatePayload } from '@/lib/api/ticketApi';
import { UtilitySplitAuditCard } from '@/components/invoice/UtilitySplitAuditCard';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { EmptyState } from '@/components/ui/EmptyState';
import { formatCurrency, getErrorMessage } from '@/lib/utils';

export default function TenantSelfPortalPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const [profile, setProfile] = useState<Tenant | null>(null);
  const [lease, setLease] = useState<Lease | null>(null);
  const [receipts, setReceipts] = useState<Receipt[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
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
      const [profileRes, leaseRes, receiptsRes, invoicesRes] = await Promise.all([
        tenantApi.getMyProfile().catch(() => null),
        leaseApi.getMyLease().catch(() => null),
        receiptApi.getMyReceipts().catch(() => []),
        invoiceApi.getMyInvoices().catch(() => []),
      ]);
      setProfile(profileRes);
      setLease(leaseRes);
      setReceipts(receiptsRes || []);
      setInvoices(invoicesRes || []);
    } catch (err) {
      console.error('Failed to load tenant portal data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleDownloadInvoicePdf = async (invoiceId: string) => {
    try {
      const blob = await invoiceApi.downloadPdf(invoiceId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Invoice_${invoiceId}.pdf`;
      a.click();
      toast.success('Invoice PDF downloaded successfully!');
    } catch (err) {
      toast.error('Failed to download invoice PDF.');
    }
  };

  const handleDownloadAgreementPdf = async () => {
    if (!lease?.id) return;
    try {
      const blob = await leaseApi.downloadPdf(lease.id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Rent_Agreement_${lease.id}.pdf`;
      a.click();
      toast.success('Rent Agreement downloaded!');
    } catch (err) {
      toast.error('Failed to download rent agreement PDF.');
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
      toast.success('Receipt PDF downloaded!');
    } catch (err) {
      toast.error('Failed to download receipt PDF.');
    }
  };

  const handleOpenTicketModal = () => {
    if (!lease || !profile) {
      toast.warning('No active lease or profile found to file a ticket.');
      return;
    }
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
      toast.success('Maintenance ticket submitted successfully!');
      setIsTicketModalOpen(false);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to submit maintenance ticket'));
    } finally {
      setIsSubmittingTicket(false);
    }
  };

  if (isLoading) {
    return (
      <div className="py-20 text-center space-y-3">
        <div className="w-8 h-8 mx-auto border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
        <p className="text-xs font-semibold text-slate-500">Loading your resident portal...</p>
      </div>
    );
  }

  // Calculate pending balance if any
  const pendingInvoices = invoices.filter((i) => i.status !== 'PAID' && i.status !== 'CANCELLED');
  const totalBalanceDue = pendingInvoices.reduce((sum, i) => sum + (i.balanceDue || i.totalAmount || 0), 0);

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-300">
      {/* Top Banner */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-slate-900 via-indigo-950 to-blue-900 text-white p-6 sm:p-8 shadow-xl border border-slate-800">
        <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="space-y-2 max-w-xl">
            <div className="flex items-center gap-2">
              <span className="px-3 py-1 bg-white/10 backdrop-blur-md text-blue-200 font-bold text-xs uppercase tracking-wider rounded-full border border-white/15">
                Resident Portal
              </span>
              {profile?.kycStatus && (
                <Badge variant={profile.kycStatus === 'VERIFIED' ? 'success' : 'warning'} dot>
                  KYC: {profile.kycStatus}
                </Badge>
              )}
            </div>
            <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white">
              Welcome home, {user?.fullName || profile?.fullName || 'Resident'}
            </h1>
            <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
              Manage your tenancy agreement, view itemized electricity & utility splits, and submit fast maintenance requests.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            {lease && (
              <Button variant="primary" onClick={handleOpenTicketModal}>
                <Wrench className="w-4 h-4 mr-1.5" /> Maintenance Request
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Balance & Tenancy Quick Highlights */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {/* Profile Card */}
        <div className="bg-white rounded-2xl p-5 sm:p-6 border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
              <User className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-slate-900 text-sm">Resident Profile</h3>
              <p className="text-[11px] text-slate-500">Contact & Verification Info</p>
            </div>
          </div>
          <div className="space-y-2 text-xs text-slate-600 pt-2 border-t border-slate-100">
            <p className="flex justify-between">
              <span className="text-slate-400">Name:</span>
              <span className="font-semibold text-slate-800">{profile?.fullName || user?.fullName}</span>
            </p>
            <p className="flex justify-between">
              <span className="text-slate-400">Email:</span>
              <span className="font-semibold text-slate-800 truncate max-w-[180px]">{profile?.email || user?.email}</span>
            </p>
            <p className="flex justify-between">
              <span className="text-slate-400">Phone:</span>
              <span className="font-semibold text-slate-800">{profile?.phone || 'N/A'}</span>
            </p>
            <p className="flex justify-between">
              <span className="text-slate-400">ID Proof:</span>
              <span className="font-semibold text-slate-800">{profile?.idProofType || 'Aadhaar'} ({profile?.idProofLast4 || 'Verified'})</span>
            </p>
          </div>
        </div>

        {/* Active Lease Agreement Card */}
        <div className="md:col-span-2 bg-white rounded-2xl p-5 sm:p-6 border border-slate-200/80 shadow-xs space-y-4 flex flex-col justify-between">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
                <FileText className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-bold text-slate-900 text-sm">Active Tenancy Agreement</h3>
                <p className="text-[11px] text-slate-500">Official digital rental contract</p>
              </div>
            </div>
            {lease && (
              <Button size="sm" variant="outline" onClick={handleDownloadAgreementPdf}>
                <Download className="w-3.5 h-3.5 mr-1.5" /> Download PDF
              </Button>
            )}
          </div>

          {!lease ? (
            <p className="text-xs text-slate-500 py-4">No active lease agreement found for your account.</p>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs pt-3 border-t border-slate-100">
              <div>
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Start Date</span>
                <p className="font-bold text-slate-900 mt-0.5">{lease.startDate}</p>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">End Date</span>
                <p className="font-bold text-slate-900 mt-0.5">{lease.endDate}</p>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Monthly Rent</span>
                <p className="font-bold text-emerald-600 text-sm mt-0.5">{formatCurrency(lease.monthlyRent)}</p>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Due Day</span>
                <p className="font-bold text-slate-900 mt-0.5">Day {lease.rentDueDay} of month</p>
              </div>
            </div>
          )}

          {/* Outstanding Balance Banner if any */}
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
              {totalBalanceDue > 0 ? (
                <>
                  <span className="w-2 h-2 rounded-full bg-amber-500" />
                  <span className="font-medium text-slate-700">Pending Balance:</span>
                  <span className="font-bold text-amber-700">{formatCurrency(totalBalanceDue)}</span>
                </>
              ) : (
                <>
                  <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                  <span className="font-semibold text-emerald-700">All invoices paid and up to date!</span>
                </>
              )}
            </div>
            <span className="text-[11px] text-slate-500 font-medium">Auto-receipt generated upon payment</span>
          </div>
        </div>
      </div>

      {/* Invoices & Utility Bills Table */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-5 sm:p-6 space-y-4 shadow-xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
              <FileSpreadsheet className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-slate-900 text-sm">Rent & Utility Bills</h3>
              <p className="text-[11px] text-slate-500">Transparent itemized bills with utility split breakdowns</p>
            </div>
          </div>
        </div>

        {invoices.length === 0 ? (
          <EmptyState
            icon={FileSpreadsheet}
            title="No Invoices Issued Yet"
            description="Your monthly rent and utility split invoices will appear here once generated by property management."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-bold text-[10px]">
                <tr>
                  <th className="px-4 py-3">Invoice #</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Period</th>
                  <th className="px-4 py-3">Due Date</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">PDF</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                {invoices.map((inv) => (
                  <React.Fragment key={inv.id}>
                    <tr className="hover:bg-slate-50/80 transition-colors">
                      <td className="px-4 py-3 font-bold text-slate-900">{inv.invoiceNumber}</td>
                      <td className="px-4 py-3">
                        <Badge
                          variant={
                            inv.invoiceType === 'UTILITY_ONLY'
                              ? 'info'
                              : inv.invoiceType === 'MONTHLY_RENT'
                              ? 'purple'
                              : 'default'
                          }
                        >
                          {inv.invoiceType}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-slate-500">
                        {inv.billingPeriodStart} to {inv.billingPeriodEnd}
                      </td>
                      <td className="px-4 py-3 text-slate-600 font-medium">{inv.dueDate}</td>
                      <td className="px-4 py-3 font-bold text-slate-900">{formatCurrency(inv.totalAmount)}</td>
                      <td className="px-4 py-3">
                        <Badge
                          variant={
                            inv.status === 'PAID'
                              ? 'success'
                              : inv.status === 'OVERDUE'
                              ? 'danger'
                              : 'warning'
                          }
                          dot
                        >
                          {inv.status}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <Button size="sm" variant="outline" onClick={() => handleDownloadInvoicePdf(inv.id)}>
                          <Download className="w-3.5 h-3.5 mr-1" /> PDF
                        </Button>
                      </td>
                    </tr>
                    {inv.metadata && (
                      <tr className="bg-slate-50/60">
                        <td colSpan={7} className="px-4 py-3">
                          <UtilitySplitAuditCard metadataString={inv.metadata} />
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Receipts History */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-5 sm:p-6 space-y-4 shadow-xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center font-bold">
              <ReceiptIcon className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-slate-900 text-sm">Payment Receipts</h3>
              <p className="text-[11px] text-slate-500">Official tax-compliant payment acknowledgments</p>
            </div>
          </div>
        </div>

        {receipts.length === 0 ? (
          <EmptyState
            icon={ReceiptIcon}
            title="No Receipts Yet"
            description="Receipts are generated immediately after any payment is confirmed by your property manager."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-bold text-[10px]">
                <tr>
                  <th className="px-4 py-3">Receipt #</th>
                  <th className="px-4 py-3">Date</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3 text-right">Download</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                {receipts.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-4 py-3 font-bold text-slate-900">{r.receiptNumber}</td>
                    <td className="px-4 py-3 text-slate-500">{r.paymentDate}</td>
                    <td className="px-4 py-3">
                      <Badge variant="purple">{r.receiptType}</Badge>
                    </td>
                    <td className="px-4 py-3 font-bold text-slate-900">{formatCurrency(r.amount)}</td>
                    <td className="px-4 py-3 text-right">
                      <Button size="sm" variant="outline" onClick={() => handleDownloadReceiptPdf(r.id)}>
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

      {/* Ticket Submission Modal */}
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
              <label className="text-xs font-bold uppercase tracking-wider text-slate-700">Category</label>
              <select
                className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white text-slate-900"
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
              <label className="text-xs font-bold uppercase tracking-wider text-slate-700">Priority</label>
              <select
                className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white text-slate-900"
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
            <label className="text-xs font-bold uppercase tracking-wider text-slate-700">Description</label>
            <textarea
              className="w-full px-3.5 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white placeholder:text-slate-400 text-slate-900"
              rows={3}
              placeholder="Describe the issue and preferred inspection time..."
              value={ticketForm.description}
              onChange={(e) => setTicketForm({ ...ticketForm, description: e.target.value })}
            />
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-slate-100">
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
