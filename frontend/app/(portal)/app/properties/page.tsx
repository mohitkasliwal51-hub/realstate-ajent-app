'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { Building2, Plus, MapPin, ArrowRight, Layers } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { propertyApi, Property, PropertyType } from '@/lib/api/propertyApi';
import { landlordApi, Landlord } from '@/lib/api/landlordApi';
import { isBrokerMode, isLandlordRequired } from '@/lib/orgMode';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';

export default function PropertiesPage() {
  const { user } = useAuth();
  const brokerMode = isBrokerMode(user);
  const landlordRequired = isLandlordRequired(user);

  const [properties, setProperties] = useState<Property[]>([]);
  const [landlords, setLandlords] = useState<Landlord[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    landlordId: '',
    name: '',
    type: 'PG' as PropertyType,
    address: '',
    city: '',
    state: '',
    pincode: '',
    landmark: '',
    description: '',
  });

  const loadProperties = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      const [propsData, landlordsData] = await Promise.all([
        propertyApi.getProperties(),
        brokerMode ? landlordApi.getLandlords().catch(() => []) : Promise.resolve([]),
      ]);
      setProperties(propsData || []);
      setLandlords(landlordsData || []);
    } catch (err) {
      console.error('Failed to load properties', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadProperties();
  }, [user?.organizationId]);

  const handleCreateProperty = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.organizationId) return;
    if (landlordRequired && !formData.landlordId) {
      alert('BROKERAGE mode requires selecting a property landlord.');
      return;
    }
    setIsSubmitting(true);
    try {
      await propertyApi.createProperty({
        ...formData,
        landlordId: formData.landlordId || undefined,
      });
      setIsModalOpen(false);
      setFormData({
        landlordId: '',
        name: '',
        type: 'PG',
        address: '',
        city: '',
        state: '',
        pincode: '',
        landmark: '',
        description: '',
      });
      await loadProperties();
    } catch (err) {
      alert('Failed to create property. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getBadgeVariant = (type: PropertyType) => {
    switch (type) {
      case 'PG': return 'purple';
      case 'HOSTEL': return 'info';
      case 'FLAT': return 'success';
      case 'COMMERCIAL': return 'warning';
      default: return 'default';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header section */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Properties</h1>
          <p className="text-xs text-slate-500 mt-1">
            Manage your real estate portfolio, PG accommodations, commercial spaces, and hostel buildings.
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>
          <Plus className="w-4 h-4 mr-2" /> Add Property
        </Button>
      </div>

      {/* Property Cards Grid */}
      {isLoading ? (
        <div className="text-center py-12 text-slate-400 text-sm">Loading properties...</div>
      ) : properties.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-slate-300 space-y-4">
          <div className="w-12 h-12 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center mx-auto">
            <Building2 className="w-6 h-6" />
          </div>
          <div>
            <h3 className="font-bold text-slate-900">No properties added yet</h3>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              Get started by creating your first property (PG, Hostel, Flat, or Commercial building).
            </p>
          </div>
          <Button onClick={() => setIsModalOpen(true)}>
            <Plus className="w-4 h-4 mr-2" /> Add Property
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {properties.map((property) => (
            <Link
              key={property.id}
              href={`/app/properties/${property.id}`}
              className="group bg-white rounded-2xl border border-slate-200 shadow-xs hover:shadow-lg hover:border-blue-500 transition-all overflow-hidden flex flex-col justify-between"
            >
              <div className="p-6 space-y-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h3 className="font-bold text-slate-900 text-lg group-hover:text-blue-600 transition-colors">
                      {property.name}
                    </h3>
                    <p className="text-xs text-slate-500 flex items-center gap-1 mt-1">
                      <MapPin className="w-3.5 h-3.5 text-slate-400" />
                      {property.address}, {property.city}
                    </p>
                  </div>
                  <Badge variant={getBadgeVariant(property.type)}>{property.type}</Badge>
                </div>

                {property.description && (
                  <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed">
                    {property.description}
                  </p>
                )}
              </div>

              <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between text-xs font-semibold text-blue-600 group-hover:text-blue-700">
                <span className="flex items-center gap-1 text-slate-600">
                  <Layers className="w-4 h-4 text-slate-400" /> Manage Units & Inventory
                </span>
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </div>
            </Link>
          ))}
        </div>
      )}

      {/* Add Property Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Add New Property"
        description="Enter property location and type details"
      >
        <form onSubmit={handleCreateProperty} className="space-y-4">
          {brokerMode && (
            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                Property Owner / Landlord {landlordRequired ? '*' : '(Optional for Self-Owned)'}
              </label>
              <select
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={formData.landlordId}
                onChange={(e) => setFormData({ ...formData, landlordId: e.target.value })}
                required={landlordRequired}
              >
                <option value="">{landlordRequired ? 'Select landlord...' : 'Self-Owned (Direct Org Property)'}</option>
                {landlords.map((l) => (
                  <option key={l.id} value={l.id}>{l.legalName} ({l.phone})</option>
                ))}
              </select>
            </div>
          )}

          <Input
            label="Property Name"
            placeholder="e.g. Green Park Residency"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            required
          />

          <div className="w-full flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Property Type
            </label>
            <select
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={formData.type}
              onChange={(e) => setFormData({ ...formData, type: e.target.value as PropertyType })}
            >
              <option value="PG">PG (Paying Guest)</option>
              <option value="HOSTEL">Hostel</option>
              <option value="FLAT">Flat / Apartment</option>
              <option value="COMMERCIAL">Commercial Office / Space</option>
            </select>
          </div>

          <Input
            label="Address"
            placeholder="Street address or block details"
            value={formData.address}
            onChange={(e) => setFormData({ ...formData, address: e.target.value })}
            required
          />

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="City"
              placeholder="e.g. Bengaluru"
              value={formData.city}
              onChange={(e) => setFormData({ ...formData, city: e.target.value })}
              required
            />
            <Input
              label="State"
              placeholder="e.g. Karnataka"
              value={formData.state}
              onChange={(e) => setFormData({ ...formData, state: e.target.value })}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Pincode"
              placeholder="560001"
              value={formData.pincode}
              onChange={(e) => setFormData({ ...formData, pincode: e.target.value })}
              required
            />
            <Input
              label="Landmark"
              placeholder="Near Metro Station"
              value={formData.landmark}
              onChange={(e) => setFormData({ ...formData, landmark: e.target.value })}
            />
          </div>

          <div className="w-full flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
              Description (Optional)
            </label>
            <textarea
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 placeholder:text-slate-400"
              rows={2}
              placeholder="Brief details about the property..."
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={isSubmitting}>
              Create Property
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
