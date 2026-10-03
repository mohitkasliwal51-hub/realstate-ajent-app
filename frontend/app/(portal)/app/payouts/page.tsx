'use client';

import React, { useEffect, useState } from 'react';
import { Wallet, Plus, Download, Calculator, AlertCircle, Building } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { payoutApi, LandlordPayout, PayoutStatus } from '@/lib/api/payoutApi';
import { landlordApi, Landlord } from '@/lib/api/landlordApi';
import { isBrokerMode } from '@/lib/orgMode';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { formatCurrency, getErrorMessage } from '@/lib/utils';

export default function PayoutsPage() {
  const { user } = useAuth();
  const brokerMode = isBrokerMode(user);

  const [payouts, setPayouts] = useState<LandlordPayout[]>([]);
  const [landlords, setLandlords] = useState<Landlord[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedLandlordId, setSelectedLandlordId] = useState('');
  const [periodMonth, setPeriodMonth] = useState(new Date().toISOString().substring(0, 7));

  const loadData = async () => {
    if (!brokerMode) {
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    try {
      const [payoutsRes, landlordsRes] = await Promise.all([
        payoutApi.getPayouts(),
        landlordApi.getLandlords(),
      ]);
      setPayouts(payoutsRes || []);
      setLandlords(landlordsRes || []);
    } catch (err) {
      console.error('Failed to load payout data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user?.organizationId]);

  const handleCalculateMonthly = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLandlordId) return;
    setIsSubmitting(true);
    try {
      await payoutApi.calculateMonthlyPayout(selectedLandlordId, periodMonth);
      setIsModalOpen(false);
      await loadData();
    } catch (err) {
      alert(getErrorMessage(err, 'Failed to calculate monthly payout'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDownloadPdf = async (id: string) => {
    try {
      const blob = await payoutApi.downloadPdf(id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Payout_${id}.pdf`;
      a.click();
    } catch (err) {
      alert('Failed to download payout statement PDF.');
    }
  };

  const getStatusBadge = (status: PayoutStatus) => {
    switch (status) {
      case 'SETTLED': return <Badge variant="success">Settled</Badge>;
      case 'PROCESSING': return <Badge variant="info">Processing</Badge>;
      case 'FAILED': return <Badge variant="danger">Failed</Badge>;
      default: return <Badge variant="warning">Pending</Badge>;
    }
  };

  if (!brokerMode) {
    return (
      <div className="bg-amber-50 border border-amber-200 rounded-2xl p-8 text-center space-y-3">
        <AlertCircle className="w-10 h-10 text-amber-600 mx-auto" />
        <h2 className="font-bold text-amber-900 text-lg">Landlord Payouts Disabled</h2>
        <p className="text-xs text-amber-700 max-w-md mx-auto">
          Landlord payouts are only applicable for <strong>Brokerage</strong> & <strong>Hybrid</strong> organizations that manage properties on behalf of external property owners. Direct OWNER accounts manage their own properties directly.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Landlord Payout Statements</h1>
          <p className="text-xs text-slate-500 mt-1">
            Automated monthly calculation of gross rent collected minus brokerage commissions & maintenance deductions.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>
          <Calculator className="w-4 h-4 mr-2" /> Calculate Monthly Payout
        </Button>
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-slate-400 text-sm">Loading payouts...</div>
      ) : payouts.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-slate-300 space-y-3">
          <Wallet className="w-12 h-12 text-slate-400 mx-auto" />
          <h3 className="font-bold text-slate-900">No payouts calculated yet</h3>
          <p className="text-xs text-slate-500">Calculate net monthly payouts for your onboarded landlords.</p>
          <Button onClick={() => setIsModalOpen(true)}>
            <Calculator className="w-4 h-4 mr-2" /> Calculate Monthly Payout
          </Button>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-semibold">
                <tr>
                  <th className="px-6 py-3.5">Payout #</th>
                  <th className="px-6 py-3.5">Landlord</th>
                  <th className="px-6 py-3.5">Gross Collected</th>
                  <th className="px-6 py-3.5">Commission</th>
                  <th className="px-6 py-3.5">Deductions</th>
                  <th className="px-6 py-3.5">Net Payable</th>
                  <th className="px-6 py-3.5">Status</th>
                  <th className="px-6 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {payouts.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="px-6 py-4">
                      <span className="font-bold text-slate-900">{p.payoutNumber}</span>
                    </td>
                    <td className="px-6 py-4 font-bold text-slate-800">
                      {p.landlordName || 'Landlord'}
                    </td>
                    <td className="px-6 py-4 text-slate-600">
                      {formatCurrency(p.totalCollected)}
                    </td>
                    <td className="px-6 py-4 text-red-600 font-medium">
                      -{formatCurrency(p.commissionAmount)}
                    </td>
                    <td className="px-6 py-4 text-red-600 font-medium">
                      -{formatCurrency(p.deductionsAmount)}
                    </td>
                    <td className="px-6 py-4 font-bold text-emerald-600 text-sm">
                      {formatCurrency(p.netPayoutAmount)}
                    </td>
                    <td className="px-6 py-4">{getStatusBadge(p.payoutStatus)}</td>
                    <td className="px-6 py-4 text-right">
                      <Button size="sm" variant="secondary" onClick={() => handleDownloadPdf(p.id)}>
                        <Download className="w-3.5 h-3.5 mr-1" /> Statement
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Calculate Payout Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Calculate Monthly Landlord Payout"
        description="Select landlord and billing month to calculate net rent collected minus fees."
      >
        <form onSubmit={handleCalculateMonthly} className="space-y-4">
          <div className="w-full flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Select Landlord *
            </label>
            <select
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={selectedLandlordId}
              onChange={(e) => setSelectedLandlordId(e.target.value)}
              required
            >
              <option value="">Select landlord...</option>
              {landlords.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.legalName} ({l.phone})
                </option>
              ))}
            </select>
          </div>

          <Input
            label="Billing Period Month (YYYY-MM) *"
            type="month"
            value={periodMonth}
            onChange={(e) => setPeriodMonth(e.target.value)}
            required
          />

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Run Payout Calculation
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
