'use client';

import React, { useEffect, useState } from 'react';
import { FileSpreadsheet, Plus, Download } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { invoiceApi, Invoice, InvoiceStatus } from '@/lib/api/invoiceApi';
import { leaseApi, Lease } from '@/lib/api/leaseApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { TableSkeleton } from '@/components/ui/TableSkeleton';
import { UtilitySplitAuditCard } from '@/components/invoice/UtilitySplitAuditCard';
import { SearchFilterBar } from '@/components/ui/SearchFilterBar';
import { EmptyState } from '@/components/ui/EmptyState';
import { formatCurrency, getErrorMessage } from '@/lib/utils';
import { useToast } from '@/lib/toast/ToastContext';

export default function InvoicesPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [leases, setLeases] = useState<Lease[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const [formData, setFormData] = useState({
    leaseId: '',
    tenantId: '',
    unitId: '',
    billingPeriodStart: new Date().toISOString().split('T')[0],
    billingPeriodEnd: new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0],
    dueDate: new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0],
    amount: 15000,
    description: 'Monthly Rent Invoice',
  });

  const isTenant = user?.role === 'TENANT';

  const loadData = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      if (isTenant) {
        const myInvoices = await invoiceApi.getMyInvoices();
        setInvoices(myInvoices || []);
      } else {
        const [invoicesRes, leasesRes] = await Promise.all([
          invoiceApi.getInvoices(),
          leaseApi.getLeases(),
        ]);
        setInvoices(invoicesRes || []);
        setLeases((leasesRes || []).filter((l) => l.status === 'ACTIVE'));
      }
    } catch (err) {
      console.error('Failed to load invoice data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user?.organizationId, user?.role]);

  const handleLeaseSelect = (leaseId: string) => {
    const lease = leases.find((l) => l.id === leaseId);
    if (lease) {
      setFormData({
        ...formData,
        leaseId: lease.id,
        tenantId: lease.tenantId,
        unitId: lease.unitId,
        amount: lease.monthlyRent,
      });
    }
  };

  const handleCreateInvoice = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await invoiceApi.createInvoice({
        leaseId: formData.leaseId,
        tenantId: formData.tenantId,
        unitId: formData.unitId,
        billingPeriodStart: formData.billingPeriodStart,
        billingPeriodEnd: formData.billingPeriodEnd,
        dueDate: formData.dueDate,
        lineItems: [
          {
            chargeType: 'RENT',
            description: formData.description,
            amount: formData.amount,
          },
        ],
      });
      toast.success('Invoice created successfully!');
      setIsModalOpen(false);
      await loadData();
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to create invoice'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDownloadPdf = async (id: string) => {
    try {
      const blob = await invoiceApi.downloadPdf(id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Invoice_${id}.pdf`;
      a.click();
      toast.success('Invoice PDF downloaded!');
    } catch (err) {
      toast.error('Failed to download invoice PDF.');
    }
  };

  const getStatusBadge = (status: InvoiceStatus) => {
    switch (status) {
      case 'PAID': return <Badge variant="success" dot>Paid</Badge>;
      case 'PARTIAL': return <Badge variant="warning" dot>Partially Paid</Badge>;
      case 'OVERDUE': return <Badge variant="danger" dot>Overdue</Badge>;
      case 'UNPAID': return <Badge variant="info" dot>Unpaid</Badge>;
      default: return <Badge variant="default">{status}</Badge>;
    }
  };

  const filteredInvoices = invoices.filter((inv) => {
    const matchesSearch =
      searchTerm === '' ||
      inv.invoiceNumber?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      inv.tenantId?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'UNPAID' && (inv.status === 'UNPAID' || inv.status === 'PARTIAL')) ||
      inv.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const filterOptions = [
    { id: 'ALL', label: 'All', count: invoices.length },
    { id: 'UNPAID', label: 'Unpaid', count: invoices.filter((i) => i.status === 'UNPAID' || i.status === 'PARTIAL').length },
    { id: 'PAID', label: 'Paid', count: invoices.filter((i) => i.status === 'PAID').length },
    { id: 'OVERDUE', label: 'Overdue', count: invoices.filter((i) => i.status === 'OVERDUE').length },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">{isTenant ? 'My Invoices' : 'Tax & Rent Invoices'}</h1>
          <p className="text-xs text-slate-500 mt-1">
            {isTenant
              ? 'View and download your itemized monthly rent and utility invoices.'
              : 'Generate itemized tax invoices for monthly rent, utilities, and agreement fees.'}
          </p>
        </div>
        {!isTenant && (
          <Button onClick={() => setIsModalOpen(true)}>
            <Plus className="w-4 h-4 mr-2" /> Create Invoice
          </Button>
        )}
      </div>

      <SearchFilterBar
        searchTerm={searchTerm}
        onSearchChange={setSearchTerm}
        placeholder="Search by invoice number..."
        filterOptions={filterOptions}
        activeFilter={statusFilter}
        onFilterChange={setStatusFilter}
      />

      {isLoading ? (
        <TableSkeleton rows={5} columns={6} />
      ) : filteredInvoices.length === 0 ? (
        <EmptyState
          icon={FileSpreadsheet}
          title={isTenant ? 'No invoices found' : 'No invoices match your filter'}
          description={
            isTenant
              ? 'You do not have any invoices at this time.'
              : invoices.length === 0
              ? 'Issue your first itemized invoice linked to an active lease.'
              : 'Try clearing your search term or switching status tabs.'
          }
          actionLabel={!isTenant && invoices.length === 0 ? 'Create Invoice' : undefined}
          onAction={!isTenant && invoices.length === 0 ? () => setIsModalOpen(true) : undefined}
        />
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <caption className="sr-only">Invoices and payment status</caption>
              <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-semibold">
                <tr>
                  <th className="px-6 py-3.5">Invoice #</th>
                  <th className="px-6 py-3.5">Billing Period</th>
                  <th className="px-6 py-3.5">Due Date</th>
                  <th className="px-6 py-3.5">Total Amount</th>
                  <th className="px-6 py-3.5">Status</th>
                  <th className="px-6 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {filteredInvoices.map((inv) => (
                  <React.Fragment key={inv.id}>
                    <tr className="hover:bg-slate-50/80 transition-colors">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-slate-900">{inv.invoiceNumber}</span>
                          {inv.invoiceType === 'UTILITY_ONLY' && (
                            <Badge variant="info">Utility Split</Badge>
                          )}
                          {inv.invoiceType === 'MONTHLY_RENT' && (
                            <Badge variant="purple">Rent</Badge>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4 text-slate-600">
                        {inv.billingPeriodStart} to {inv.billingPeriodEnd}
                      </td>
                      <td className="px-6 py-4 text-slate-600">{inv.dueDate}</td>
                      <td className="px-6 py-4 font-bold text-slate-900">
                        {formatCurrency(inv.totalAmount)}
                      </td>
                      <td className="px-6 py-4">{getStatusBadge(inv.status)}</td>
                      <td className="px-6 py-4 text-right">
                        <Button size="sm" variant="secondary" onClick={() => handleDownloadPdf(inv.id)}>
                          <Download className="w-3.5 h-3.5 mr-1" /> PDF
                        </Button>
                      </td>
                    </tr>
                    {inv.metadata && (
                      <tr className="bg-slate-50/30">
                        <td colSpan={6} className="px-6 py-1.5">
                          <UtilitySplitAuditCard metadataString={inv.metadata} />
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Create Rent Invoice"
        description="Select an active lease to populate tenant details"
      >
        <form onSubmit={handleCreateInvoice} className="space-y-4">
          <div className="w-full flex flex-col gap-1.5">
            <label htmlFor="invoice-lease" className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Select Active Lease
            </label>
            <select
              id="invoice-lease"
              name="leaseId"
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={formData.leaseId}
              onChange={(e) => handleLeaseSelect(e.target.value)}
              required
            >
              <option value="">Select lease...</option>
              {leases.map((l) => (
                <option key={l.id} value={l.id}>
                  Lease #{l.id.substring(0, 8)} ({l.status}) - {formatCurrency(l.monthlyRent)}/mo
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Billing Start Date"
              type="date"
              value={formData.billingPeriodStart}
              onChange={(e) => setFormData({ ...formData, billingPeriodStart: e.target.value })}
              required
            />
            <Input
              label="Billing End Date"
              type="date"
              value={formData.billingPeriodEnd}
              onChange={(e) => setFormData({ ...formData, billingPeriodEnd: e.target.value })}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Due Date"
              type="date"
              value={formData.dueDate}
              onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
              required
            />
            <Input
              label="Rent Amount (₹)"
              type="number"
              value={formData.amount}
              onChange={(e) => setFormData({ ...formData, amount: parseFloat(e.target.value) || 0 })}
              required
            />
          </div>

          <Input
            label="Description"
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
          />

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Generate Invoice
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
