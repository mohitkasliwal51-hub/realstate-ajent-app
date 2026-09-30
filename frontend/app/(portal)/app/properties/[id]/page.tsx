'use client';

import React, { use, useEffect, useState } from 'react';
import Link from 'next/link';
import {
  Building2,
  Plus,
  MapPin,
  ArrowLeft,
  Filter,
  CheckCircle2,
  AlertTriangle,
  UserX,
  Wrench,
  Ban,
  Layers,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { propertyApi, Property } from '@/lib/api/propertyApi';
import { unitApi, Unit, UnitStatus, SharingType } from '@/lib/api/unitApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { formatCurrency } from '@/lib/utils';

export default function PropertyDetailsPage({
  params: paramsPromise,
}: {
  params: Promise<{ id: string }>;
}) {
  const params = use(paramsPromise);
  const propertyId = params.id;

  const { user } = useAuth();
  const [property, setProperty] = useState<Property | null>(null);
  const [units, setUnits] = useState<Unit[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  const [isUnitModalOpen, setIsUnitModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [unitFormData, setUnitFormData] = useState({
    unitNumber: '',
    floorNumber: 0,
    sharingType: 'FULL_FLAT' as SharingType,
    monthlyRent: 15000,
    securityDeposit: 30000,
    status: 'AVAILABLE' as UnitStatus,
    amenities: '',
    notes: '',
  });

  const loadData = async () => {
    if (!user?.organizationId || !propertyId) return;
    setIsLoading(true);
    try {
      const [prop, unitList] = await Promise.all([
        propertyApi.getPropertyById(propertyId, user.organizationId),
        unitApi.getUnitsByProperty(propertyId, user.organizationId),
      ]);
      setProperty(prop);
      setUnits(unitList || []);
    } catch (err) {
      console.error('Failed to load property details', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [propertyId, user?.organizationId]);

  const handleCreateUnit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.organizationId || !propertyId) return;
    setIsSubmitting(true);
    try {
      await unitApi.createUnit({
        organizationId: user.organizationId,
        propertyId,
        ...unitFormData,
      });
      setIsUnitModalOpen(false);
      setUnitFormData({
        unitNumber: '',
        floorNumber: 0,
        sharingType: 'FULL_FLAT',
        monthlyRent: 15000,
        securityDeposit: 30000,
        status: 'AVAILABLE',
        amenities: '',
        notes: '',
      });
      await loadData();
    } catch (err) {
      alert('Failed to add unit. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleStatusChange = async (unitId: string, newStatus: UnitStatus) => {
    if (!user?.organizationId) return;
    try {
      await unitApi.updateUnitStatus(unitId, user.organizationId, newStatus);
      await loadData();
    } catch (err) {
      alert('Failed to update status.');
    }
  };

  const availableCount = units.filter((u) => u.status === 'AVAILABLE').length;
  const occupiedCount = units.filter((u) => u.status === 'OCCUPIED').length;
  const reservedCount = units.filter((u) => u.status === 'RESERVED').length;
  const maintenanceCount = units.filter((u) => u.status === 'MAINTENANCE').length;
  const disabledCount = units.filter((u) => u.status === 'DISABLED').length;

  const filteredUnits = units.filter((u) => {
    if (statusFilter === 'ALL') return true;
    return u.status === statusFilter;
  });

  const getStatusBadge = (status: UnitStatus) => {
    switch (status) {
      case 'AVAILABLE':
        return <Badge variant="success">Available</Badge>;
      case 'OCCUPIED':
        return <Badge variant="danger">Occupied</Badge>;
      case 'RESERVED':
        return <Badge variant="warning">Reserved</Badge>;
      case 'MAINTENANCE':
        return <Badge variant="info">Maintenance</Badge>;
      case 'DISABLED':
        return <Badge variant="default">Disabled</Badge>;
    }
  };

  if (isLoading) {
    return <div className="text-center py-12 text-slate-400 text-sm">Loading inventory...</div>;
  }

  if (!property) {
    return <div className="text-center py-12 text-slate-500">Property not found.</div>;
  }

  return (
    <div className="space-y-8">
      {/* Top Header */}
      <div>
        <Link
          href="/app/properties"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-blue-600 mb-3 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" /> Back to Properties
        </Link>

        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-slate-900 tracking-tight">{property.name}</h1>
              <Badge variant="purple">{property.type}</Badge>
            </div>
            <p className="text-xs text-slate-500 flex items-center gap-1 mt-1">
              <MapPin className="w-3.5 h-3.5 text-slate-400" />
              {property.address}, {property.city}, {property.state} - {property.pincode}
            </p>
          </div>

          <Button onClick={() => setIsUnitModalOpen(true)}>
            <Plus className="w-4 h-4 mr-2" /> Add Unit / Room / Bed
          </Button>
        </div>
      </div>

      {/* Vacancy & Occupancy Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs">
          <p className="text-xs text-slate-500 font-medium">Total Inventory</p>
          <p className="text-xl font-bold text-slate-900 mt-0.5">{units.length}</p>
        </div>

        <div className="bg-emerald-50/70 p-4 rounded-xl border border-emerald-200">
          <p className="text-xs text-emerald-700 font-semibold flex items-center gap-1">
            <CheckCircle2 className="w-3.5 h-3.5" /> Available
          </p>
          <p className="text-xl font-bold text-emerald-900 mt-0.5">{availableCount}</p>
        </div>

        <div className="bg-rose-50/70 p-4 rounded-xl border border-rose-200">
          <p className="text-xs text-rose-700 font-semibold flex items-center gap-1">
            <UserX className="w-3.5 h-3.5" /> Occupied
          </p>
          <p className="text-xl font-bold text-rose-900 mt-0.5">{occupiedCount}</p>
        </div>

        <div className="bg-amber-50/70 p-4 rounded-xl border border-amber-200">
          <p className="text-xs text-amber-700 font-semibold flex items-center gap-1">
            <AlertTriangle className="w-3.5 h-3.5" /> Reserved
          </p>
          <p className="text-xl font-bold text-amber-900 mt-0.5">{reservedCount}</p>
        </div>

        <div className="bg-slate-100 p-4 rounded-xl border border-slate-300">
          <p className="text-xs text-slate-700 font-semibold flex items-center gap-1">
            <Wrench className="w-3.5 h-3.5" /> Maintenance / Off
          </p>
          <p className="text-xl font-bold text-slate-900 mt-0.5">
            {maintenanceCount + disabledCount}
          </p>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-200 pb-2 overflow-x-auto">
        {['ALL', 'AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE', 'DISABLED'].map((status) => (
          <button
            key={status}
            onClick={() => setStatusFilter(status)}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
              statusFilter === status
                ? 'bg-slate-900 text-white'
                : 'text-slate-600 hover:bg-slate-200/60'
            }`}
          >
            {status} ({status === 'ALL' ? units.length : units.filter((u) => u.status === status).length})
          </button>
        ))}
      </div>

      {/* Units Inventory Grid */}
      {filteredUnits.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-slate-300 space-y-3">
          <Layers className="w-10 h-10 text-slate-400 mx-auto" />
          <h3 className="font-bold text-slate-900 text-sm">No units found in this view</h3>
          <p className="text-xs text-slate-500">Add rooms, beds, or flat units to manage inventory.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredUnits.map((unit) => (
            <div
              key={unit.id}
              className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs hover:border-blue-400 transition-all flex flex-col justify-between space-y-4"
            >
              <div className="space-y-3">
                <div className="flex items-start justify-between">
                  <div>
                    <h4 className="font-bold text-slate-900 text-base">Unit {unit.unitNumber}</h4>
                    <p className="text-xs text-slate-500">Floor {unit.floorNumber} • {unit.sharingType}</p>
                  </div>
                  {getStatusBadge(unit.status)}
                </div>

                <div className="grid grid-cols-2 gap-2 bg-slate-50 p-3 rounded-xl text-xs">
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-semibold">Rent</span>
                    <span className="font-bold text-slate-800">{formatCurrency(unit.monthlyRent)}/mo</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-semibold">Deposit</span>
                    <span className="font-bold text-slate-800">{formatCurrency(unit.securityDeposit)}</span>
                  </div>
                </div>

                {unit.notes && (
                  <p className="text-xs text-slate-500 italic bg-slate-50 p-2 rounded-lg">
                    "{unit.notes}"
                  </p>
                )}
              </div>

              {/* Status change actions */}
              <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="text-[11px] font-semibold text-slate-400">Change status:</span>
                <select
                  value={unit.status}
                  onChange={(e) => handleStatusChange(unit.id, e.target.value as UnitStatus)}
                  className="px-2 py-1 bg-slate-100 text-slate-700 rounded-md text-xs font-semibold focus:outline-none focus:ring-1 focus:ring-blue-500"
                >
                  <option value="AVAILABLE">AVAILABLE</option>
                  <option value="OCCUPIED">OCCUPIED</option>
                  <option value="RESERVED">RESERVED</option>
                  <option value="MAINTENANCE">MAINTENANCE</option>
                  <option value="DISABLED">DISABLED</option>
                </select>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Unit Modal */}
      <Modal
        isOpen={isUnitModalOpen}
        onClose={() => setIsUnitModalOpen(false)}
        title="Add Unit / Room / Bed"
        description={`Configure inventory item inside ${property.name}`}
      >
        <form onSubmit={handleCreateUnit} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Unit / Room Number"
              placeholder="e.g. 101, Bed-A"
              value={unitFormData.unitNumber}
              onChange={(e) => setUnitFormData({ ...unitFormData, unitNumber: e.target.value })}
              required
            />
            <Input
              label="Floor Number"
              type="number"
              value={unitFormData.floorNumber}
              onChange={(e) => setUnitFormData({ ...unitFormData, floorNumber: parseInt(e.target.value) || 0 })}
              required
            />
          </div>

          <div className="w-full flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Sharing / Unit Type
            </label>
            <select
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={unitFormData.sharingType}
              onChange={(e) => setUnitFormData({ ...unitFormData, sharingType: e.target.value as SharingType })}
            >
              <option value="FULL_FLAT">Full Flat / Entire Unit</option>
              <option value="SINGLE">Single Room</option>
              <option value="DOUBLE">Double Sharing</option>
              <option value="TRIPLE">Triple Sharing</option>
              <option value="FOUR_SHARING">Four Sharing</option>
              <option value="COMMERCIAL_SPACE">Commercial Space</option>
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Monthly Rent (₹)"
              type="number"
              value={unitFormData.monthlyRent}
              onChange={(e) => setUnitFormData({ ...unitFormData, monthlyRent: parseFloat(e.target.value) || 0 })}
              required
            />
            <Input
              label="Security Deposit (₹)"
              type="number"
              value={unitFormData.securityDeposit}
              onChange={(e) => setUnitFormData({ ...unitFormData, securityDeposit: parseFloat(e.target.value) || 0 })}
              required
            />
          </div>

          <div className="w-full flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Initial Status
            </label>
            <select
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={unitFormData.status}
              onChange={(e) => setUnitFormData({ ...unitFormData, status: e.target.value as UnitStatus })}
            >
              <option value="AVAILABLE">AVAILABLE</option>
              <option value="OCCUPIED">OCCUPIED</option>
              <option value="RESERVED">RESERVED</option>
              <option value="MAINTENANCE">MAINTENANCE</option>
              <option value="DISABLED">DISABLED</option>
            </select>
          </div>

          <Input
            label="Notes / Furnishing Details"
            placeholder="e.g. AC, Attached Bath, Balcony"
            value={unitFormData.notes}
            onChange={(e) => setUnitFormData({ ...unitFormData, notes: e.target.value })}
          />

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setIsUnitModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Add Unit to Inventory
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
