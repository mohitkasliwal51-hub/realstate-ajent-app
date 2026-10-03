'use client';

import React, { useEffect, useState } from 'react';
import { Zap, Plus, Droplets, Flame, AlertCircle } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { meterReadingApi, MeterReading, MeterType } from '@/lib/api/meterReadingApi';
import { propertyApi } from '@/lib/api/propertyApi';
import { unitApi, Unit } from '@/lib/api/unitApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { getErrorMessage } from '@/lib/utils';

export default function MeterReadingsPage() {
  const { user } = useAuth();
  const [readings, setReadings] = useState<MeterReading[]>([]);
  const [units, setUnits] = useState<Unit[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    unitId: '',
    meterType: 'ELECTRICITY' as MeterType,
    previousReading: 0,
    currentReading: 100,
    readingDate: new Date().toISOString().split('T')[0],
    ratePerUnit: 8.5,
    notes: '',
  });

  const loadData = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      const [readingsRes, propertiesRes] = await Promise.all([
        meterReadingApi.getMeterReadings(),
        propertyApi.getProperties(),
      ]);
      setReadings(readingsRes || []);
      const unitsArrays = await Promise.all(
        (propertiesRes || []).map((p) => unitApi.getUnitsByProperty(p.id))
      );
      setUnits(unitsArrays.flat() || []);
    } catch (err) {
      console.error('Failed to load meter reading data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user?.organizationId]);

  const handleCreateReading = async (e: React.FormEvent) => {
    e.preventDefault();
    if (formData.currentReading < formData.previousReading) {
      alert('Current reading cannot be less than previous reading.');
      return;
    }
    setIsSubmitting(true);
    try {
      await meterReadingApi.createMeterReading(formData);
      setIsModalOpen(false);
      await loadData();
    } catch (err) {
      alert(getErrorMessage(err, 'Failed to log meter reading'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const getMeterIcon = (type: MeterType) => {
    switch (type) {
      case 'WATER': return <Droplets className="w-4 h-4 text-cyan-600" />;
      case 'GAS': return <Flame className="w-4 h-4 text-amber-600" />;
      default: return <Zap className="w-4 h-4 text-yellow-600" />;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Utility & Meter Readings</h1>
          <p className="text-xs text-slate-500 mt-1">
            Log monthly electricity, water, and gas meter readings for auto-utility billing.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>
          <Plus className="w-4 h-4 mr-2" /> Log Meter Reading
        </Button>
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-slate-400 text-sm">Loading meter readings...</div>
      ) : readings.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-slate-300 space-y-3">
          <Zap className="w-12 h-12 text-slate-400 mx-auto" />
          <h3 className="font-bold text-slate-900">No meter readings logged</h3>
          <p className="text-xs text-slate-500">Record electricity or water meter units for occupied units.</p>
          <Button onClick={() => setIsModalOpen(true)}>
            <Plus className="w-4 h-4 mr-2" /> Log Meter Reading
          </Button>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-semibold">
                <tr>
                  <th className="px-6 py-3.5">Reading Date</th>
                  <th className="px-6 py-3.5">Unit</th>
                  <th className="px-6 py-3.5">Utility Type</th>
                  <th className="px-6 py-3.5">Previous Reading</th>
                  <th className="px-6 py-3.5">Current Reading</th>
                  <th className="px-6 py-3.5">Units Consumed</th>
                  <th className="px-6 py-3.5">Total Charge</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {readings.map((r) => {
                  const consumed = Math.max(0, (r.currentReading || 0) - (r.previousReading || 0));
                  const charge = consumed * (r.ratePerUnit || 8.5);
                  return (
                    <tr key={r.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="px-6 py-4 font-bold text-slate-900">{r.readingDate}</td>
                      <td className="px-6 py-4 text-slate-800 font-semibold">Unit {r.unitNumber || 'N/A'}</td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-1.5 font-semibold text-slate-800">
                          {getMeterIcon(r.meterType)} {r.meterType}
                        </div>
                      </td>
                      <td className="px-6 py-4 text-slate-600">{r.previousReading}</td>
                      <td className="px-6 py-4 text-slate-900 font-bold">{r.currentReading}</td>
                      <td className="px-6 py-4 font-bold text-blue-600">{consumed} units</td>
                      <td className="px-6 py-4 font-bold text-slate-900">₹{charge.toFixed(2)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Log Utility Meter Reading"
        description="Select unit and enter current meter reading."
      >
        <form onSubmit={handleCreateReading} className="space-y-4">
          <div className="w-full flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Select Unit *
            </label>
            <select
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={formData.unitId}
              onChange={(e) => setFormData({ ...formData, unitId: e.target.value })}
              required
            >
              <option value="">Select unit...</option>
              {units.map((u) => (
                <option key={u.id} value={u.id}>
                  Unit {u.unitNumber} ({u.status})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">Utility Type</label>
              <select
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={formData.meterType}
                onChange={(e) => setFormData({ ...formData, meterType: e.target.value as MeterType })}
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
              onChange={(e) => setFormData({ ...formData, previousReading: parseFloat(e.target.value) || 0 })}
              required
            />
            <Input
              label="Current Reading"
              type="number"
              value={formData.currentReading}
              onChange={(e) => setFormData({ ...formData, currentReading: parseFloat(e.target.value) || 0 })}
              required
            />
            <Input
              label="Rate per Unit (₹)"
              type="number"
              step="0.01"
              value={formData.ratePerUnit}
              onChange={(e) => setFormData({ ...formData, ratePerUnit: parseFloat(e.target.value) || 0 })}
              required
            />
          </div>

          <Input
            label="Notes (Optional)"
            placeholder="e.g. Sub-meter reading taken during monthly audit"
            value={formData.notes}
            onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
          />

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Save Meter Reading
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
