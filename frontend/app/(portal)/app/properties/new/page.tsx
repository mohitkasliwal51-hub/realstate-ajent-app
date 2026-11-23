'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  Building2,
  MapPin,
  Wifi,
  Zap,
  Shield,
  Waves,
  Sparkles,
  Clock,
  Ban,
  Check,
  X,
  Layers,
  CheckCircle2,
  Image as ImageIcon,
  ArrowLeft,
  ArrowRight,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { propertyApi, PropertyCreatePayload, PropertyType } from '@/lib/api/propertyApi';
import { landlordApi, Landlord } from '@/lib/api/landlordApi';
import { useAuth } from '@/lib/auth/AuthContext';
import { isBrokerMode, isLandlordRequired } from '@/lib/orgMode';
import { useToast } from '@/lib/toast/ToastContext';
import { validatePincode } from '@/lib/validation';

const COMMON_AMENITIES = [
  { id: 'wifi', label: 'High-Speed Wi-Fi', icon: '📶' },
  { id: 'power_backup', label: '24/7 Power Backup (DG)', icon: '⚡' },
  { id: 'cctv', label: 'CCTV & Security', icon: '🛡️' },
  { id: 'lift', label: 'Elevator / Lift', icon: '🛗' },
  { id: 'ro_water', label: 'RO Drinking Water', icon: '💧' },
  { id: 'housekeeping', label: 'Daily Housekeeping', icon: '🧹' },
  { id: 'washing_machine', label: 'Washing Machine', icon: '🧺' },
  { id: 'air_conditioning', label: 'Air Conditioning (AC)', icon: '❄️' },
  { id: 'geyser', label: 'Hot Water Geyser', icon: '🚿' },
  { id: 'gym', label: 'Fitness Gym', icon: '🏋️' },
  { id: 'two_wheeler_parking', label: '2-Wheeler Parking', icon: '🛵' },
  { id: 'four_wheeler_parking', label: '4-Wheeler Car Parking', icon: '🚗' },
];

const COMMON_RULES = [
  { id: 'gate_curfew_11pm', label: 'Gate Closes at 11:00 PM' },
  { id: 'no_curfew', label: '24/7 Open (No Curfew)' },
  { id: 'non_veg_allowed', label: 'Non-Veg Cooking Allowed' },
  { id: 'veg_only', label: 'Strictly Vegetarian Only' },
  { id: 'no_smoking', label: 'No Smoking Inside Rooms' },
  { id: 'no_alcohol', label: 'No Alcohol Inside Premises' },
  { id: 'visitors_allowed_day', label: 'Day Visitors Allowed (Till 8 PM)' },
  { id: 'no_overnight_guests', label: 'No Overnight Guests Allowed' },
];

export default function NewPropertyPage() {
  const router = useRouter();
  const { user } = useAuth();
  const { toast } = useToast();

  const brokerMode = isBrokerMode(user);
  const landlordRequired = isLandlordRequired(user);
  const [landlords, setLandlords] = useState<Landlord[]>([]);

  const [activeTab, setActiveTab] = useState<'basic' | 'amenities' | 'rules' | 'photos'>('basic');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    name: '',
    type: 'PG' as PropertyType,
    landlordId: '',
    address: '',
    city: '',
    state: '',
    pincode: '',
    landmark: '',
    description: '',
    images: [] as string[],
    selectedAmenities: [] as string[],
    selectedRules: [] as string[],
  });

  useEffect(() => {
    if (brokerMode) {
      landlordApi.getLandlords().then(setLandlords).catch(() => []);
    }
  }, [brokerMode]);

  const toggleAmenity = (amenityId: string) => {
    setFormData((prev) => ({
      ...prev,
      selectedAmenities: prev.selectedAmenities.includes(amenityId)
        ? prev.selectedAmenities.filter((a) => a !== amenityId)
        : [...prev.selectedAmenities, amenityId],
    }));
  };

  const toggleRule = (ruleId: string) => {
    setFormData((prev) => ({
      ...prev,
      selectedRules: prev.selectedRules.includes(ruleId)
        ? prev.selectedRules.filter((r) => r !== ruleId)
        : [...prev.selectedRules, ruleId],
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.name.trim()) {
      toast.warning('Property name is required.');
      setActiveTab('basic');
      return;
    }

    if (landlordRequired && !formData.landlordId) {
      toast.warning('BROKERAGE mode requires selecting a property landlord.');
      setActiveTab('basic');
      return;
    }

    if (!formData.address.trim() || !formData.city.trim()) {
      toast.warning('Street address and city are required.');
      setActiveTab('basic');
      return;
    }

    if (formData.pincode && !validatePincode(formData.pincode)) {
      toast.warning('Please enter a valid 6-digit Indian PIN code.');
      setActiveTab('basic');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload: PropertyCreatePayload = {
        name: formData.name.trim(),
        type: formData.type,
        landlordId: formData.landlordId ? formData.landlordId : undefined,
        address: formData.address.trim(),
        city: formData.city.trim(),
        state: formData.state.trim() || 'Maharashtra',
        pincode: formData.pincode.trim() || '411057',
        landmark: formData.landmark.trim() || undefined,
        description: formData.description.trim() || undefined,
        amenities: JSON.stringify(formData.selectedAmenities),
        rules: formData.selectedRules,
        images: formData.images,
      };

      await propertyApi.createProperty(payload);
      toast.success('Property successfully listed!');
      router.push('/app/properties');
    } catch (err: any) {
      toast.error('Failed to create property. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-in fade-in duration-300 pb-16">
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <Link
          href="/app/properties"
          className="text-xs font-semibold text-slate-500 hover:text-slate-800 flex items-center gap-1"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Properties
        </Link>
        <Badge variant="purple">Property Wizard</Badge>
      </div>

      <div className="space-y-1">
        <h1 className="text-2xl font-black tracking-tight text-slate-900">
          List a New Property / Co-Living Space
        </h1>
        <p className="text-xs text-slate-500">
          Configure property specifications, amenities, house rules, and photos for your dedicated website and marketplace.
        </p>
      </div>

      {/* Tabs */}
      <div role="tablist" aria-label="Property configuration steps" className="flex border-b border-slate-200 gap-2 text-xs font-bold">
        {[
          { id: 'basic', label: '1. Basic & Location' },
          { id: 'amenities', label: '2. Amenities & Perks' },
          { id: 'rules', label: '3. House Rules' },
          { id: 'photos', label: '4. Photos & Media' },
        ].map((tab) => (
          <button
            key={tab.id}
            id={`property-${tab.id}-tab`}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            aria-current={activeTab === tab.id ? 'step' : undefined}
            aria-controls={`property-${tab.id}-panel`}
            onClick={() => setActiveTab(tab.id as any)}
            className={`py-3 px-4 border-b-2 transition-all whitespace-nowrap ${
              activeTab === tab.id
                ? 'border-blue-600 text-blue-600 bg-blue-50/40 rounded-t-xl'
                : 'border-transparent text-slate-500 hover:text-slate-900'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="space-y-6" aria-label="New property details">
        {/* TAB 1: BASIC & LOCATION */}
        {activeTab === 'basic' && (
          <div
            id="property-basic-panel"
            role="tabpanel"
            aria-labelledby="property-basic-tab"
            className="p-6 rounded-3xl bg-white border border-slate-200 shadow-sm space-y-5 animate-in fade-in duration-200"
          >
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="sm:col-span-2">
                <Input
                  label="Property / Residency Name"
                  placeholder="e.g. Greenwood Co-Living Hub"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>

              <div className="space-y-1.5">
                <label htmlFor="property-category" className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                  Property Category
                </label>
                <select
                  id="property-category"
                  name="propertyType"
                  value={formData.type}
                  onChange={(e) => setFormData({ ...formData, type: e.target.value as PropertyType })}
                  className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="PG">PG / Co-Living (Bed/Room Sharing)</option>
                  <option value="FLAT">Full Apartment / Flat</option>
                  <option value="HOSTEL">Hostel / Student Residency</option>
                  <option value="COMMERCIAL">Commercial / Office</option>
                </select>
              </div>
            </div>

            {brokerMode && (
              <div className="space-y-1.5">
                <label htmlFor="property-landlord" className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                  Select Landlord Owner {landlordRequired && <span className="text-rose-500">*</span>}
                </label>
                <select
                  id="property-landlord"
                  name="landlordId"
                  value={formData.landlordId}
                  onChange={(e) => setFormData({ ...formData, landlordId: e.target.value })}
                  className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required={landlordRequired}
                >
                  <option value="">-- Choose Landlord Client --</option>
                  {landlords.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.legalName} ({l.phone})
                    </option>
                  ))}
                </select>
              </div>
            )}

            <Input
              label="Street Address / Building"
              placeholder="e.g. 42, Hinjewadi Phase 1, Near IT Park"
              value={formData.address}
              onChange={(e) => setFormData({ ...formData, address: e.target.value })}
              required
            />

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <Input
                label="City"
                placeholder="e.g. Pune"
                value={formData.city}
                onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                required
              />
              <Input
                label="State"
                placeholder="e.g. Maharashtra"
                value={formData.state}
                onChange={(e) => setFormData({ ...formData, state: e.target.value })}
              />
              <Input
                label="Pincode"
                placeholder="e.g. 411057"
                value={formData.pincode}
                onChange={(e) => setFormData({ ...formData, pincode: e.target.value })}
                maxLength={6}
              />
            </div>

            <Input
              label="Prominent Landmark"
              placeholder="e.g. Behind Vivanta Hotel"
              value={formData.landmark}
              onChange={(e) => setFormData({ ...formData, landmark: e.target.value })}
            />

            <div className="space-y-1.5">
              <label htmlFor="property-description" className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                Property Overview & Marketing Description
              </label>
              <textarea
                id="property-description"
                name="description"
                rows={3}
                placeholder="Mention highlights such as food mess, metro connectivity, private balconies..."
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 placeholder:text-slate-400"
              />
            </div>

            <div className="flex justify-end pt-2">
              <Button type="button" onClick={() => setActiveTab('amenities')}>
                Next: Select Amenities ➔
              </Button>
            </div>
          </div>
        )}

        {/* TAB 2: AMENITIES */}
        {activeTab === 'amenities' && (
          <div
            id="property-amenities-panel"
            role="tabpanel"
            aria-labelledby="property-amenities-tab"
            className="p-6 rounded-3xl bg-white border border-slate-200 shadow-sm space-y-5 animate-in fade-in duration-200"
          >
            <div>
              <h3 className="text-sm font-bold text-slate-900">Select Available Property Amenities</h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Badges will be displayed prominently on your showcase website and explore listings.
              </p>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {COMMON_AMENITIES.map((item) => {
                const isSelected = formData.selectedAmenities.includes(item.id);
                return (
                  <button
                    type="button"
                    key={item.id}
                    onClick={() => toggleAmenity(item.id)}
                    className={`p-3 rounded-2xl border text-left flex items-center justify-between transition-all ${
                      isSelected
                        ? 'border-blue-600 bg-blue-50/60 shadow-xs ring-1 ring-blue-500'
                        : 'border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <span className="text-xs font-semibold text-slate-800 flex items-center gap-2">
                      <span>{item.icon}</span> {item.label}
                    </span>
                    {isSelected && <Check className="w-4 h-4 text-blue-600" />}
                  </button>
                );
              })}
            </div>

            <div className="flex justify-between pt-2">
              <Button type="button" variant="outline" onClick={() => setActiveTab('basic')}>
                <ArrowLeft className="w-4 h-4 mr-1" /> Back
              </Button>
              <Button type="button" onClick={() => setActiveTab('rules')}>
                Next: House Rules ➔
              </Button>
            </div>
          </div>
        )}

        {/* TAB 3: HOUSE RULES */}
        {activeTab === 'rules' && (
          <div
            id="property-rules-panel"
            role="tabpanel"
            aria-labelledby="property-rules-tab"
            className="p-6 rounded-3xl bg-white border border-slate-200 shadow-sm space-y-5 animate-in fade-in duration-200"
          >
            <div>
              <h3 className="text-sm font-bold text-slate-900">House Rules & Tenancy Policies</h3>
              <p className="text-xs text-slate-500 mt-0.5">
                These guidelines are automatically incorporated into the digital lease agreement e-signed by residents.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {COMMON_RULES.map((rule) => {
                const isSelected = formData.selectedRules.includes(rule.id);
                return (
                  <button
                    type="button"
                    key={rule.id}
                    onClick={() => toggleRule(rule.id)}
                    className={`p-3 rounded-2xl border text-left flex items-center justify-between transition-all ${
                      isSelected
                        ? 'border-purple-600 bg-purple-50/60 shadow-xs ring-1 ring-purple-500'
                        : 'border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <span className="text-xs font-semibold text-slate-800">{rule.label}</span>
                    {isSelected && <Check className="w-4 h-4 text-purple-600" />}
                  </button>
                );
              })}
            </div>

            <div className="flex justify-between pt-2">
              <Button type="button" variant="outline" onClick={() => setActiveTab('amenities')}>
                <ArrowLeft className="w-4 h-4 mr-1" /> Back
              </Button>
              <Button type="button" onClick={() => setActiveTab('photos')}>
                Next: Photos & Publish ➔
              </Button>
            </div>
          </div>
        )}

        {/* TAB 4: PHOTOS & MEDIA */}
        {activeTab === 'photos' && (
          <div
            id="property-photos-panel"
            role="tabpanel"
            aria-labelledby="property-photos-tab"
            className="p-6 rounded-3xl bg-white border border-slate-200 shadow-sm space-y-5 animate-in fade-in duration-200"
          >
            <div>
              <h3 className="text-sm font-bold text-slate-900">Property Photos & Gallery</h3>
              <p className="text-xs text-slate-500 mt-0.5">
                High-quality photos increase token reservations by 3.5x.
              </p>
            </div>

            <div className="border-2 border-dashed border-slate-200 rounded-3xl p-8 text-center space-y-3 bg-slate-50">
              <div className="w-12 h-12 rounded-2xl bg-white border border-slate-200 text-slate-400 flex items-center justify-center mx-auto shadow-xs">
                <ImageIcon className="w-6 h-6" />
              </div>
              <div>
                <p className="text-xs font-bold text-slate-800">Upload Room & Property Images</p>
                <p className="text-[11px] text-slate-400 mt-0.5">JPEG, PNG or WEBP up to 5MB each</p>
              </div>
              <Button type="button" variant="outline" size="sm" className="bg-white">
                Choose Files
              </Button>
            </div>

            <div className="flex justify-between pt-2">
              <Button type="button" variant="outline" onClick={() => setActiveTab('rules')}>
                <ArrowLeft className="w-4 h-4 mr-1" /> Back
              </Button>
              <Button type="submit" isLoading={isSubmitting} className="font-bold px-8 shadow-md shadow-blue-500/20">
                Publish Property Listing ➔
              </Button>
            </div>
          </div>
        )}
      </form>
    </div>
  );
}
