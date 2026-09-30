'use client';

import React, { useEffect, useState } from 'react';
import { Receipt as ReceiptIcon, Plus, Download, CreditCard, CheckCircle2 } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { receiptApi, Receipt, ReceiptType, PaymentMode } from '@/lib/api/receiptApi';
import { leaseApi, Lease } from '@/lib/api/leaseApi';
import { tenantApi, Tenant } from '@/lib/api/tenantApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { formatCurrency, getErrorMessage } from '@/lib/utils';

export default function ReceiptsPage() {
  const { user } = useAuth();
  const [receipts, setReceipts] = useState<Receipt[]>([]);
  const [leases, setLeases] = useState<Lease[]>([]);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    leaseId: '',
    tenantId: '',
    type: 'RENT_PAYMENT' as ReceiptType,
    amount: 15000,
    paymentMode: 'UPI' as PaymentMode,
    transactionRef: '',
    notes: 'Monthly rent payment received via StayFile OS.',
  });

  const loadData = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      const [receiptsRes, leasesRes, tenantsRes] = await Promise.all([
        receiptApi.getReceipts(user.organizationId),
        leaseApi.getLeases(user.organizationId),
        tenantApi.getTenants(user.organizationId),
      ]);
      setReceipts(receiptsRes || []);
      setLeases(leasesRes || []);
      setTenants(tenantsRes || []);
    } catch (err) {
      console.error('Failed to load receipt data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user?.organizationId]);

  const handleLeaseSelect = (leaseId: string) => {
    const selectedLease = leases.find((l) => l.id === leaseId);
    if (selectedLease) {
      setFormData({
        ...formData,
        leaseId: selectedLease.id,
        tenantId: selectedLease.tenantId,
        amount: selectedLease.monthlyRent,
      });
    }
  };

  const handleCreateReceipt = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.organizationId) return;
    setIsSubmitting(true);
    try {
      await receiptApi.createReceipt({
        organizationId: user.organizationId,
        ownerId: user.id,
        ...formData,
      });
      setIsModalOpen(false);
      await loadData();
    } catch (err: unknown) {
      alert(getErrorMessage(err, 'Failed to generate receipt.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDownloadPdf = async (receiptId: string) => {
    if (!user?.organizationId) return;
    try {
      const blob = await receiptApi.downloadPdf(receiptId, user.organizationId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Receipt_${receiptId}.pdf`;
      a.click();
    } catch (err) {
      alert('Failed to download receipt PDF.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Rent Receipts</h1>
          <p className="text-xs text-slate-500 mt-1">
            Receipts created exclusively from active valid leases with automatic tenant mapping & PDF export.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>
          <Plus className="w-4 h-4 mr-2" /> Issue Rent Receipt
        </Button>
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-slate-400 text-sm">Loading receipts...</div>
      ) : receipts.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-slate-300 space-y-4">
          <ReceiptIcon className="w-12 h-12 text-slate-400 mx-auto" />
          <div>
            <h3 className="font-bold text-slate-900">No receipts issued yet</h3>
            <p className="text-xs text-slate-500 mt-1">Select an active lease to issue your first rent receipt.</p>
          </div>
          <Button onClick={() => setIsModalOpen(true)}>
            <Plus className="w-4 h-4 mr-2" /> Issue Rent Receipt
          </Button>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-semibold">
                <tr>
                  <th className="px-6 py-3.5">Receipt #</th>
                  <th className="px-6 py-3.5">Type & Mode</th>
                  <th className="px-6 py-3.5">Amount Paid</th>
                  <th className="px-6 py-3.5">Transaction Ref</th>
                  <th className="px-6 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {receipts.map((receipt) => (
                  <tr key={receipt.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                        <span className="font-bold text-slate-900">{receipt.receiptNumber}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 space-y-1">
                      <Badge variant="purple">{receipt.type}</Badge>
                      <p className="text-slate-500 text-[11px] font-semibold">{receipt.paymentMode}</p>
                    </td>
                    <td className="px-6 py-4 font-bold text-slate-900 text-sm">
                      {formatCurrency(receipt.amount)}
                    </td>
                    <td className="px-6 py-4 text-slate-600">
                      {receipt.transactionRef || 'N/A'}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Button size="sm" variant="secondary" onClick={() => handleDownloadPdf(receipt.id)}>
                        <Download className="w-3.5 h-3.5 mr-1" /> PDF
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Generate Receipt Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Issue Rent Receipt"
        description="Select an active lease to auto-populate tenant details"
      >
        <form onSubmit={handleCreateReceipt} className="space-y-4">
          <div className="w-full flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              1. Select Valid Lease
            </label>
            <select
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={formData.leaseId}
              onChange={(e) => handleLeaseSelect(e.target.value)}
              required
            >
              <option value="">Select lease...</option>
              {leases.map((l) => (
                <option key={l.id} value={l.id}>Lease #{l.id.substring(0, 8)} ({l.status}) - {formatCurrency(l.monthlyRent)}/mo</option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                Receipt Type
              </label>
              <select
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={formData.type}
                onChange={(e) => setFormData({ ...formData, type: e.target.value as ReceiptType })}
              >
                <option value="RENT_PAYMENT">Rent Payment</option>
                <option value="SECURITY_DEPOSIT">Security Deposit</option>
                <option value="MAINTENANCE">Maintenance Fee</option>
                <option value="UTILITY_BILL">Utility Charges</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                Payment Mode
              </label>
              <select
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={formData.paymentMode}
                onChange={(e) => setFormData({ ...formData, paymentMode: e.target.value as PaymentMode })}
              >
                <option value="UPI">UPI / GPay / PhonePe</option>
                <option value="BANK_TRANSFER">Bank Transfer / NEFT / IMPS</option>
                <option value="RAZORPAY_ONLINE">Online Gateway</option>
                <option value="CASH">Cash</option>
                <option value="CHEQUE">Cheque</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Amount Paid (₹)"
              type="number"
              value={formData.amount}
              onChange={(e) => setFormData({ ...formData, amount: parseFloat(e.target.value) || 0 })}
              required
            />
            <Input
              label="Transaction Ref / UTR #"
              placeholder="e.g. UPI/123456789"
                value={formData.transactionRef}
                onChange={(e) => setFormData({ ...formData, transactionRef: e.target.value })}
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Generate Receipt & PDF
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
