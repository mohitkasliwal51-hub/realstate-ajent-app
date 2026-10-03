'use client';

import React, { use, useEffect, useState } from 'react';
import Link from 'next/link';
import { Building2, MapPin, Phone, Mail, Globe, ArrowRight } from 'lucide-react';
import { orgApi, ShowcaseData } from '@/lib/api/orgApi';
import { leadApi } from '@/lib/api/leadApi';
import { Property } from '@/lib/api/propertyApi';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';

export default function PublicShowcasePage({
  params: paramsPromise,
}: {
  params: Promise<{ organizationSlug: string }>;
}) {
  const params = use(paramsPromise);
  const organizationSlug = params.organizationSlug;

  const [showcase, setShowcase] = useState<ShowcaseData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedProperty, setSelectedProperty] = useState<Property | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState(false);

  const [inquiryData, setInquiryData] = useState({
    name: '',
    phone: '',
    email: '',
    notes: '',
  });

  useEffect(() => {
    const loadShowcase = async () => {
      setIsLoading(true);
      try {
        const data = await orgApi.getPublicShowcase(organizationSlug);
        setShowcase(data);
      } catch (err) {
        console.error('Failed to load showcase', err);
      } finally {
        setIsLoading(false);
      }
    };
    loadShowcase();
  }, [organizationSlug]);

  const handleOpenInquiry = (property: Property) => {
    setSelectedProperty(property);
    setIsModalOpen(true);
    setSuccessMessage(false);
  };

  const handleInquirySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!showcase) return;
    setIsSubmitting(true);
    try {
      await leadApi.createLead({
        propertyId: selectedProperty?.id || '',
        name: inquiryData.name,
        phone: inquiryData.phone,
        email: inquiryData.email,
        source: 'WEBSITE',
        notes: inquiryData.notes || `Inquiry for ${selectedProperty?.name}`,
      });
      setSuccessMessage(true);
      setInquiryData({ name: '', phone: '', email: '', notes: '' });
    } catch (err) {
      alert('Failed to submit inquiry. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white text-sm">
        Loading organization showcase...
      </div>
    );
  }

  if (!showcase) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-900 text-white p-6 text-center space-y-4">
        <h1 className="text-2xl font-bold">Organization Showcase Not Found</h1>
        <p className="text-xs text-slate-400">Slug "{organizationSlug}" could not be retrieved.</p>
        <Link href="/login">
          <Button variant="primary">Return to Portal</Button>
        </Link>
      </div>
    );
  }

  const primaryColor = showcase.branding?.primaryColor || '#2563eb';

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      {/* Brand Header */}
      <header className="bg-slate-900 text-white py-12 px-6 shadow-xl relative overflow-hidden">
        <div className="max-w-6xl mx-auto flex flex-col md:flex-row items-start md:items-center justify-between gap-6 relative z-10">
          <div className="flex items-center gap-4">
            <div
              className="w-14 h-14 rounded-2xl flex items-center justify-center text-white font-extrabold text-2xl shadow-lg"
              style={{ backgroundColor: primaryColor }}
            >
              {showcase.organizationName.charAt(0)}
            </div>
            <div>
              <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight">
                {showcase.branding?.tradeName || showcase.organizationName}
              </h1>
              {showcase.branding?.legalBusinessName && (
                <p className="text-xs text-slate-400 mt-0.5">
                  Legal entity: {showcase.branding.legalBusinessName}
                </p>
              )}
            </div>
          </div>

          <div className="flex items-center gap-6 text-xs text-slate-300">
            {showcase.branding?.contactPhone && (
              <span className="flex items-center gap-1.5">
                <Phone className="w-4 h-4 text-blue-400" /> {showcase.branding.contactPhone}
              </span>
            )}
            {showcase.branding?.contactEmail && (
              <span className="flex items-center gap-1.5">
                <Mail className="w-4 h-4 text-blue-400" /> {showcase.branding.contactEmail}
              </span>
            )}
          </div>
        </div>
      </header>

      {/* Properties Showcase Grid */}
      <main className="max-w-6xl mx-auto px-6 py-12 space-y-8">
        <div className="space-y-2">
          <h2 className="text-2xl font-bold tracking-tight text-slate-900">Available Properties</h2>
          <p className="text-xs text-slate-500">
            Browse verified rental accommodations, PGs, flats, and commercial spaces.
          </p>
        </div>

        {showcase.properties.length === 0 ? (
          <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center text-slate-500 text-sm">
            No active properties currently listed for this organization.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {showcase.properties.map((property) => (
              <div
                key={property.id}
                className="bg-white rounded-2xl border border-slate-200 shadow-xs hover:shadow-lg transition-all p-6 space-y-4 flex flex-col justify-between"
              >
                <div className="space-y-3">
                  <div className="flex items-start justify-between">
                    <h3 className="font-bold text-lg text-slate-900">{property.name}</h3>
                    <Badge variant="purple">{property.type}</Badge>
                  </div>
                  <p className="text-xs text-slate-500 flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-slate-400" />
                    {property.address}, {property.city}
                  </p>
                  {property.description && (
                    <p className="text-xs text-slate-600 line-clamp-3 leading-relaxed">
                      {property.description}
                    </p>
                  )}
                </div>

                <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-xs font-semibold text-emerald-600">Verified Listing</span>
                  <Button size="sm" variant="outline" onClick={() => handleOpenInquiry(property)}>
                    Inquire Now
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Inquiry Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={`Inquire for ${selectedProperty?.name || 'Property'}`}
        description="Fill details to schedule a site visit or get details via WhatsApp"
      >
        {successMessage ? (
          <div className="p-6 text-center space-y-3">
            <div className="w-12 h-12 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto font-bold text-xl">
              ✓
            </div>
            <h3 className="font-bold text-slate-900">Inquiry Submitted!</h3>
            <p className="text-xs text-slate-500">
              The property manager will contact you shortly via phone or WhatsApp.
            </p>
            <Button onClick={() => setIsModalOpen(false)}>Close</Button>
          </div>
        ) : (
          <form onSubmit={handleInquirySubmit} className="space-y-4">
            <Input
              label="Your Full Name"
              placeholder="e.g. Ankit Sharma"
              value={inquiryData.name}
              onChange={(e) => setInquiryData({ ...inquiryData, name: e.target.value })}
              required
            />
            <Input
              label="Phone Number"
              type="tel"
              placeholder="+91 98765 43210"
              value={inquiryData.phone}
              onChange={(e) => setInquiryData({ ...inquiryData, phone: e.target.value })}
              required
            />
            <Input
              label="Email Address"
              type="email"
              placeholder="ankit@example.com"
              value={inquiryData.email}
              onChange={(e) => setInquiryData({ ...inquiryData, email: e.target.value })}
            />
            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                Message / Preferred Move-in Date
              </label>
              <textarea
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 placeholder:text-slate-400"
                rows={3}
                placeholder="Looking for single sharing / 2BHK from next month..."
                value={inquiryData.notes}
                onChange={(e) => setInquiryData({ ...inquiryData, notes: e.target.value })}
              />
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" isLoading={isSubmitting}>
                Submit Inquiry
              </Button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
}
