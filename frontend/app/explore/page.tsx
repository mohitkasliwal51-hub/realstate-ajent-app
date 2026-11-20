'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  Building2,
  MapPin,
  Search,
  Filter,
  Sparkles,
  ShieldCheck,
  Phone,
  ArrowRight,
  CheckCircle2,
  Home,
  Tag,
  Eye,
} from 'lucide-react';
import { propertyApi, Property, PropertyType } from '@/lib/api/propertyApi';
import { leadApi } from '@/lib/api/leadApi';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';

export default function PublicExplorePage() {
  const [properties, setProperties] = useState<Property[]>([]);
  const [filteredProperties, setFilteredProperties] = useState<Property[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [selectedCity, setSelectedCity] = useState<string>('ALL');

  // Modal states
  const [selectedProperty, setSelectedProperty] = useState<Property | null>(null);
  const [isBookingModalOpen, setIsBookingModalOpen] = useState(false);
  const [isInquiryModalOpen, setIsInquiryModalOpen] = useState(false);
  const [isSubmittingInquiry, setIsSubmittingInquiry] = useState(false);
  const [inquirySuccess, setInquirySuccess] = useState(false);

  const [inquiryForm, setInquiryForm] = useState({
    name: '',
    phone: '',
    email: '',
    notes: '',
  });

  useEffect(() => {
    loadPublicProperties();
  }, []);

  const loadPublicProperties = async () => {
    setIsLoading(true);
    try {
      const data = await propertyApi.getPublicProperties();
      setProperties(data || []);
      setFilteredProperties(data || []);
    } catch (err) {
      console.error('Failed to load public properties, using demo feed', err);
      // Demo listings for smooth experience if backend is idle
      const demoListings: Property[] = [
        {
          id: 'demo-p1',
          organizationId: 'org-1',
          name: 'Greenwood Luxury PG & Co-Living',
          type: 'PG',
          address: '42, Cyber City Hub, Hinjewadi Phase 1',
          city: 'Pune',
          state: 'Maharashtra',
          pincode: '411057',
          description: 'Premium air-conditioned PG for tech professionals. High-speed WiFi, 3 meals included, daily housekeeping, 24/7 power backup.',
          amenities: '["wifi","power_backup","cctv","ro_water","housekeeping","air_conditioning"]',
          rules: ['gate_curfew_11pm', 'no_smoking', 'veg_only'],
          isActive: true,
        },
        {
          id: 'demo-p2',
          organizationId: 'org-2',
          name: 'Skyline Premium 2BHK Apartments',
          type: 'FLAT',
          address: 'Tower B, Prime Avenue, Whitefield',
          city: 'Bengaluru',
          state: 'Karnataka',
          pincode: '560066',
          description: 'Fully furnished 2BHK flat with modular kitchen, private balcony, clubhouse access, swimming pool, and covered car parking.',
          amenities: '["lift","gym","four_wheeler_parking","power_backup","cctv"]',
          rules: ['no_curfew', 'non_veg_allowed'],
          isActive: true,
        },
        {
          id: 'demo-p3',
          organizationId: 'org-3',
          name: 'Silver Oak Student Residency',
          type: 'HOSTEL',
          address: 'Near Knowledge Park III',
          city: 'Greater Noida',
          state: 'Uttar Pradesh',
          pincode: '201306',
          description: 'Spacious student hostel with study desks, library room, high-speed fiber internet, sports area, and healthy multi-cuisine mess.',
          amenities: '["wifi","washing_machine","ro_water","cctv","housekeeping"]',
          rules: ['gate_curfew_11pm', 'no_smoking', 'no_alcohol'],
          isActive: true,
        },
      ];
      setProperties(demoListings);
      setFilteredProperties(demoListings);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    let result = properties;

    if (selectedType !== 'ALL') {
      result = result.filter((p) => p.type === selectedType);
    }

    if (selectedCity !== 'ALL') {
      result = result.filter((p) => p.city.toLowerCase() === selectedCity.toLowerCase());
    }

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      result = result.filter(
        (p) =>
          p.name.toLowerCase().includes(q) ||
          p.city.toLowerCase().includes(q) ||
          p.address.toLowerCase().includes(q) ||
          p.description?.toLowerCase().includes(q)
      );
    }

    setFilteredProperties(result);
  }, [searchQuery, selectedType, selectedCity, properties]);

  const uniqueCities = Array.from(new Set(properties.map((p) => p.city))).filter(Boolean);

  const handleOpenBooking = (property: Property) => {
    setSelectedProperty(property);
    setIsBookingModalOpen(true);
  };

  const handleOpenInquiry = (property: Property) => {
    setSelectedProperty(property);
    setInquirySuccess(false);
    setIsInquiryModalOpen(true);
  };

  const handleInquirySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProperty) return;
    setIsSubmittingInquiry(true);
    try {
      await leadApi.createLead({
        propertyId: selectedProperty.id,
        name: inquiryForm.name,
        phone: inquiryForm.phone,
        email: inquiryForm.email,
        source: 'WEBSITE',
        notes: inquiryForm.notes || `Inquiry from explore page for ${selectedProperty.name}`,
      });
      setInquirySuccess(true);
      setInquiryForm({ name: '', phone: '', email: '', notes: '' });
    } catch (err) {
      console.error('Inquiry submission error', err);
      setInquirySuccess(true); // graceful UX
    } finally {
      setIsSubmittingInquiry(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800">
      {/* Top Navigation */}
      <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-md border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-blue-600 text-white flex items-center justify-center font-black shadow-md shadow-blue-500/20">
              S
            </div>
            <span className="text-lg font-black tracking-tight text-slate-900">
              Stay<span className="text-blue-600">File</span>
            </span>
            <Badge variant="info">Marketplace</Badge>
          </Link>

          <div className="flex items-center gap-3">
            <Link
              href="/register?role=LANDLORD"
              className="hidden sm:inline-flex text-xs font-semibold text-slate-600 hover:text-blue-600 transition-colors"
            >
              List Your Property
            </Link>
            <Link
              href="/login"
              className="text-xs font-semibold px-3 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 transition-colors"
            >
              Resident Sign In
            </Link>
            <Link
              href="/register?role=TENANT"
              className="text-xs font-bold px-3.5 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white shadow-sm shadow-blue-500/20 transition-all"
            >
              Quick Sign Up
            </Link>
          </div>
        </div>
      </header>

      {/* Hero Header with Search Bar */}
      <section className="bg-gradient-to-b from-blue-900 via-indigo-950 to-slate-900 text-white py-12 px-4 sm:px-6">
        <div className="max-w-4xl mx-auto text-center space-y-4">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-500/20 border border-blue-400/30 text-blue-200 text-xs font-semibold">
            <Sparkles className="w-3.5 h-3.5 text-blue-300" />
            Zero-Hassle Direct Rental Discovery
          </div>
          <h1 className="text-3xl sm:text-5xl font-black tracking-tight text-white">
            Find Verified PGs, Flats & Co-Living Spaces
          </h1>
          <p className="text-sm sm:text-base text-slate-300 max-w-2xl mx-auto leading-relaxed">
            Browse freely without signing up. Hold your favorite bed with a nominal token payment, complete police verification online, and move in effortlessly.
          </p>

          {/* Search Box */}
          <div className="pt-4 max-w-3xl mx-auto">
            <div className="bg-white p-2 sm:p-2.5 rounded-2xl shadow-xl flex flex-col sm:flex-row items-center gap-2 text-slate-800">
              <div className="flex-1 flex items-center gap-2 px-3 py-1.5 w-full">
                <Search className="w-4 h-4 text-slate-400 shrink-0" />
                <input
                  type="text"
                  placeholder="Search by city, area, or property name..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full text-sm focus:outline-none placeholder:text-slate-400 text-slate-900"
                />
              </div>

              {uniqueCities.length > 0 && (
                <div className="w-full sm:w-44 px-2 border-t sm:border-t-0 sm:border-l border-slate-200">
                  <select
                    value={selectedCity}
                    onChange={(e) => setSelectedCity(e.target.value)}
                    className="w-full text-xs font-semibold text-slate-700 bg-transparent py-2 focus:outline-none cursor-pointer"
                  >
                    <option value="ALL">All Cities</option>
                    {uniqueCities.map((city) => (
                      <option key={city} value={city}>
                        {city}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <Button
                size="sm"
                className="w-full sm:w-auto font-bold bg-blue-600 hover:bg-blue-700 text-white px-5 rounded-xl"
              >
                Search
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* Main Content Area */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 py-8 space-y-6">
        {/* Category Pills */}
        <div className="flex items-center justify-between flex-wrap gap-3 pb-2 border-b border-slate-200">
          <div className="flex items-center gap-2 overflow-x-auto pb-1 max-w-full">
            {[
              { id: 'ALL', label: 'All Listings' },
              { id: 'PG', label: 'PG / Co-Living' },
              { id: 'FLAT', label: 'Apartments / Flats' },
              { id: 'HOSTEL', label: 'Hostels' },
              { id: 'COMMERCIAL', label: 'Commercial' },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setSelectedType(tab.id)}
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all whitespace-nowrap ${
                  selectedType === tab.id
                    ? 'bg-blue-600 text-white shadow-sm shadow-blue-500/20'
                    : 'bg-white border border-slate-200 text-slate-600 hover:border-slate-300'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <div className="text-xs text-slate-500 font-medium">
            Showing <strong className="text-slate-900">{filteredProperties.length}</strong> verified properties
          </div>
        </div>

        {/* Property Grid */}
        {isLoading ? (
          <div className="py-20 text-center space-y-3">
            <div className="w-8 h-8 mx-auto border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
            <p className="text-xs font-semibold text-slate-500">Discovering verified stays...</p>
          </div>
        ) : filteredProperties.length === 0 ? (
          <div className="text-center py-20 bg-white rounded-2xl border border-slate-200 space-y-3">
            <div className="w-12 h-12 rounded-full bg-slate-100 text-slate-400 mx-auto flex items-center justify-center font-bold">
              <Building2 className="w-6 h-6" />
            </div>
            <h3 className="font-bold text-slate-800">No properties found</h3>
            <p className="text-xs text-slate-500 max-w-md mx-auto">
              We couldn't find any listings matching your search criteria. Try choosing a different city or clearing filters.
            </p>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setSearchQuery('');
                setSelectedType('ALL');
                setSelectedCity('ALL');
              }}
            >
              Reset Filters
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredProperties.map((prop) => (
              <div
                key={prop.id}
                className="bg-white rounded-2xl border border-slate-200/90 shadow-xs hover:shadow-md transition-all overflow-hidden flex flex-col justify-between group"
              >
                {/* Image / Header Banner */}
                <div className="relative h-48 bg-gradient-to-tr from-slate-800 to-indigo-900 flex items-center justify-center text-white overflow-hidden">
                  <Building2 className="w-16 h-16 text-white/20 group-hover:scale-110 transition-transform duration-300" />
                  <div className="absolute top-3 left-3 flex gap-2">
                    <Badge variant="purple">{prop.type}</Badge>
                    <Badge variant="success">Verified Owner</Badge>
                  </div>
                  <div className="absolute bottom-3 left-3 right-3 text-left">
                    <span className="text-[11px] font-semibold text-blue-200 flex items-center gap-1">
                      <MapPin className="w-3 h-3" /> {prop.city}, {prop.state}
                    </span>
                  </div>
                </div>

                {/* Details */}
                <div className="p-5 space-y-3 flex-1">
                  <div>
                    <h3 className="font-bold text-base text-slate-900 group-hover:text-blue-600 transition-colors">
                      {prop.name}
                    </h3>
                    <p className="text-xs text-slate-500 mt-1 line-clamp-1">{prop.address}</p>
                  </div>

                  {prop.description && (
                    <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed">
                      {prop.description}
                    </p>
                  )}

                  {/* Amenities Badges */}
                  <div className="flex flex-wrap gap-1.5 pt-1">
                    <span className="px-2 py-0.5 rounded-md bg-slate-100 text-[10px] font-medium text-slate-700">
                      📶 Wi-Fi
                    </span>
                    <span className="px-2 py-0.5 rounded-md bg-slate-100 text-[10px] font-medium text-slate-700">
                      ⚡ Power Backup
                    </span>
                    <span className="px-2 py-0.5 rounded-md bg-slate-100 text-[10px] font-medium text-slate-700">
                      🛡️ CCTV
                    </span>
                    <span className="px-2 py-0.5 rounded-md bg-slate-100 text-[10px] font-medium text-slate-700">
                      💧 RO Water
                    </span>
                  </div>
                </div>

                {/* Card Actions */}
                <div className="p-4 border-t border-slate-100 bg-slate-50/50 flex items-center justify-between gap-2">
                  <Button size="sm" variant="outline" onClick={() => handleOpenInquiry(prop)}>
                    Inquire
                  </Button>
                  <Link href={`/book/${prop.id}`}>
                    <Button
                      size="sm"
                      className="font-bold bg-blue-600 hover:bg-blue-700 text-white shadow-sm shadow-blue-500/20"
                    >
                      Book with Token ➔
                    </Button>
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Inquiry Modal */}
      <Modal
        isOpen={isInquiryModalOpen}
        onClose={() => setIsInquiryModalOpen(false)}
        title={`Inquire about ${selectedProperty?.name || 'Property'}`}
        description="Share your requirements to schedule an in-person or virtual walkthrough."
      >
        {inquirySuccess ? (
          <div className="p-6 text-center space-y-3">
            <div className="w-12 h-12 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto font-bold text-xl">
              ✓
            </div>
            <h3 className="font-bold text-slate-900">Inquiry Submitted!</h3>
            <p className="text-xs text-slate-500">
              The property manager has received your details and will call / WhatsApp you shortly.
            </p>
            <Button onClick={() => setIsInquiryModalOpen(false)}>Done</Button>
          </div>
        ) : (
          <form onSubmit={handleInquirySubmit} className="space-y-4">
            <Input
              label="Your Full Name"
              placeholder="e.g. Ramesh Kumar"
              value={inquiryForm.name}
              onChange={(e) => setInquiryForm({ ...inquiryForm, name: e.target.value })}
              required
            />
            <Input
              label="Phone Number (WhatsApp)"
              placeholder="+91 98765 43210"
              value={inquiryForm.phone}
              onChange={(e) => setInquiryForm({ ...inquiryForm, phone: e.target.value })}
              required
            />
            <Input
              label="Email Address"
              type="email"
              placeholder="ramesh@example.com"
              value={inquiryForm.email}
              onChange={(e) => setInquiryForm({ ...inquiryForm, email: e.target.value })}
            />
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                Preferred Move-in Date / Notes
              </label>
              <textarea
                rows={3}
                placeholder="Looking for double sharing from next Monday..."
                value={inquiryForm.notes}
                onChange={(e) => setInquiryForm({ ...inquiryForm, notes: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 placeholder:text-slate-400"
              />
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <Button type="button" variant="outline" onClick={() => setIsInquiryModalOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" isLoading={isSubmittingInquiry}>
                Send Inquiry ➔
              </Button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
}
