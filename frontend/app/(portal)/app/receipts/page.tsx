'use client';

import React, { useEffect, useState } from 'react';
import { Receipt as ReceiptIcon, Plus, Download, CreditCard, CheckCircle2 } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { receiptApi, Receipt, ReceiptType, PaymentMode } from '@/lib/api/receiptApi';
import { leaseApi, Lease } from '@/lib/api/leaseApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { TableSkeleton } from '@/components/ui/TableSkeleton';
import { SearchFilterBar } from '@/components/ui/SearchFilterBar';
import { EmptyState } from '@/components/ui/EmptyState';
import { formatCurrency, getErrorMessage } from '@/lib/utils';
import { useToast } from '@/lib/toast/ToastContext';

export default function ReceiptsPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const [receipts, setReceipts] = useState<Receipt[]>([]);
  const [leases, setLeases] = useState<Lease[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('ALL');

  const [formData, setFormData] = useState({
    leaseId: '',
    tenantId: '',
    receiptType: 'RENT_PAYMENT' as ReceiptType,
    amount: 15000,
    paymentMode: 'UPI' as PaymentMode,
    transactionReference: '',
    notes: 'Monthly rent payment received via StayFile OS.',
  });

  const isTenant = user?.role === 'TENANT';

  const loadData = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      if (isTenant) {
        const myReceipts = await receiptApi.getMyReceipts();
        setReceipts(myReceipts || []);
      } else {
        const [receiptsRes, leasesRes] = await Promise.all([
          receiptApi.getReceipts(),
          leaseApi.getLeases(),
        ]);
        setReceipts(receiptsRes || []);
        setLeases((leasesRes || []).filter((lease) => lease.status === 'ACTIVE'));
      }
    } catch (err) {
      console.error('Failed to load receipt data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user?.organizationId, user?.role]);

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
      const targetTenantId = formData.tenantId || leases.find((lease) => lease.id === formData.leaseId)?.tenantId || '';
      await receiptApi.createReceipt({
        ...formData,
        tenantId: targetTenantId,
      });
      toast.success('Rent receipt issued successfully!');
      setIsModalOpen(false);
      await loadData();
    } catch (err: unknown) {
      toast.error(getErrorMessage(err, 'Failed to generate receipt.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDownloadPdf = async (receiptId: string) => {
    if (!user?.organizationId) return;
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

  const filteredReceipts = receipts.filter((r) => {
    const matchesSearch =
      searchTerm === '' ||
      r.receiptNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      r.transactionReference?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesType = typeFilter === 'ALL' || r.receiptType === typeFilter;
    return matchesSearch && matchesType;
  });

  const filterOptions = [
    { id: 'ALL', label: 'All', count: receipts.length },
    { id: 'RENT_PAYMENT', label: 'Rent', count: receipts.filter((r) => r.receiptType === 'RENT_PAYMENT').length },
    { id: 'SECURITY_DEPOSIT', label: 'Deposit', count: receipts.filter((r) => r.receiptType === 'SECURITY_DEPOSIT').length },
    { id: 'MAINTENANCE', label: 'Maintenance', count: receipts.filter((r) => r.receiptType === 'MAINTENANCE').length },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">{isTenant ? 'Payment Receipts' : 'Rent Receipts'}</h1>
          <p className="text-xs text-slate-500 mt-1">
            {isTenant
              ? 'View and download official payment receipts for rent, deposits, and maintenance.'
              : 'Receipts created exclusively from active valid leases with automatic tenant mapping & PDF export.'}
          </p>
        </div>
        {!isTenant && (
          <Button onClick={() => setIsModalOpen(true)}>
            <Plus className="w-4 h-4 mr-2" /> Issue Rent Receipt
          </Button>
        )}
      </div>

      <SearchFilterBar
        searchTerm={searchTerm}
        onSearchChange={setSearchTerm}
        placeholder="Search receipts by number or reference..."
        filterOptions={filterOptions}
        activeFilter={typeFilter}
        onFilterChange={setTypeFilter}
      />

      {isLoading ? (
        <TableSkeleton rows={5} columns={5} />
      ) : filteredReceipts.length === 0 ? (
        <EmptyState
          icon={ReceiptIcon}
          title={isTenant ? 'No receipts found' : 'No receipts match your filter'}
          description={
            isTenant
              ? 'You do not have any payment receipts recorded yet.'
              : receipts.length === 0
              ? 'Select an active lease to issue your first rent receipt.'
              : 'Try clearing your search term or selecting a different filter.'
          }
          actionLabel={!isTenant && receipts.length === 0 ? 'Issue Rent Receipt' : undefined}
          onAction={!isTenant && receipts.length === 0 ? () => setIsModalOpen(true) : undefined}
        />
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
                {filteredReceipts.map((receipt) => (
                  <tr key={receipt.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                        <span className="font-bold text-slate-900">{receipt.receiptNumber}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 space-y-1">
                      <Badge variant="purple">{receipt.receiptType}</Badge>
                      <p className="text-slate-500 text-[11px] font-semibold">{receipt.paymentMode}</p>
                    </td>
                    <td className="px-6 py-4 font-bold text-slate-900 text-sm">
                      {formatCurrency(receipt.amount)}
                    </td>
                    <td className="px-6 py-4 text-slate-600">
                      {receipt.transactionReference || 'N/A'}
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
                value={formData.receiptType}
                onChange={(e) => setFormData({ ...formData, receiptType: e.target.value as ReceiptType })}
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
                <option value="NET_BANKING">Net Banking</option>
                <option value="CREDIT_CARD">Credit Card</option>
                <option value="DEBIT_CARD">Debit Card</option>
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
                value={formData.transactionReference}
                onChange={(e) => setFormData({ ...formData, transactionReference: e.target.value })}
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
