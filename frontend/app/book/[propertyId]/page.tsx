'use client';

import React, { use, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import {
  Building2,
  MapPin,
  Calendar,
  CheckCircle2,
  CreditCard,
  ShieldCheck,
  User,
  ArrowRight,
  ArrowLeft,
  Sparkles,
  Phone,
  Mail,
  Home,
} from 'lucide-react';
import { propertyApi, Property } from '@/lib/api/propertyApi';
import { unitApi, Unit } from '@/lib/api/unitApi';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { formatCurrency } from '@/lib/utils';
import { useToast } from '@/lib/toast/ToastContext';

export default function PropertyBookingPage({
  params: paramsPromise,
}: {
  params: Promise<{ propertyId: string }>;
}) {
  const params = use(paramsPromise);
  const propertyId = params.propertyId;
  const router = useRouter();
  const searchParams = useSearchParams();
  const preselectedUnitId = searchParams.get('unitId');
  const { toast } = useToast();

  const [property, setProperty] = useState<Property | null>(null);
  const [units, setUnits] = useState<Unit[]>([]);
  const [selectedUnit, setSelectedUnit] = useState<Unit | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Stepper: 1: Select Unit, 2: Guest Details, 3: Success Confirmation
  const [currentStep, setCurrentStep] = useState<1 | 2 | 3>(1);
  const [isProcessing, setIsProcessing] = useState(false);

  const [guestDetails, setGuestDetails] = useState({
    fullName: '',
    phone: '',
    email: '',
    moveInDate: '',
    tokenAmount: 2000,
  });

  useEffect(() => {
    loadPropertyAndUnits();
  }, [propertyId]);

  const loadPropertyAndUnits = async () => {
    setIsLoading(true);
    try {
      // 1. Fetch property
      let prop: Property | null = null;
      try {
        prop = await propertyApi.getPropertyById(propertyId);
      } catch {
        // Fallback from public listings
        const publicProps = await propertyApi.getPublicProperties().catch(() => []);
        prop = publicProps.find((p) => p.id === propertyId) || null;
      }

      if (!prop) {
        // Fallback mock property
        prop = {
          id: propertyId,
          organizationId: 'org-1',
          name: 'Greenwood Luxury PG & Co-Living',
          type: 'PG',
          address: '42, Hinjewadi Phase 1, Cyber City',
          city: 'Pune',
          state: 'Maharashtra',
          pincode: '411057',
          description: 'Premium air-conditioned PG with 3 meals, daily housekeeping, 24/7 power backup, and fast WiFi.',
          isActive: true,
        };
      }
      setProperty(prop);

      // 2. Fetch available units
      try {
        const unitsData = await unitApi.getUnitsByProperty(propertyId, 'AVAILABLE');
        if (unitsData && unitsData.length > 0) {
          setUnits(unitsData);
          const match = preselectedUnitId
            ? unitsData.find((u) => u.id === preselectedUnitId)
            : unitsData[0];
          setSelectedUnit(match || unitsData[0]);
        } else {
          throw new Error('No units');
        }
      } catch {
        // Mock units for demo
        const demoUnits: Unit[] = [
          {
            id: 'demo-u1',
            propertyId,
            organizationId: prop?.organizationId || '',
            unitNumber: 'Room 101 • Bed A',
            floorNumber: 1,
            sharingType: 'DOUBLE',
            monthlyRent: 8500,
            securityDeposit: 17000,
            status: 'AVAILABLE',
            notes: 'AC, Attached Washroom, Balcony',
          },
          {
            id: 'demo-u2',
            propertyId,
            organizationId: prop?.organizationId || '',
            unitNumber: 'Room 102 • Bed B',
            floorNumber: 1,
            sharingType: 'DOUBLE',
            monthlyRent: 8500,
            securityDeposit: 17000,
            status: 'AVAILABLE',
            notes: 'AC, High Speed WiFi, Wardrobe',
          },
          {
            id: 'demo-u3',
            propertyId,
            organizationId: prop?.organizationId || '',
            unitNumber: 'Room 201 • Private Room',
            floorNumber: 2,
            sharingType: 'SINGLE',
            monthlyRent: 14000,
            securityDeposit: 28000,
            status: 'AVAILABLE',
            notes: 'Private Room, Work Desk, Geyser, Fridge',
          },
        ];
        setUnits(demoUnits);
        const match = preselectedUnitId
          ? demoUnits.find((u) => u.id === preselectedUnitId)
          : demoUnits[0];
        setSelectedUnit(match || demoUnits[0]);
      }
    } catch (err) {
      console.error('Failed to load booking details', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handlePayToken = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!guestDetails.fullName || !guestDetails.phone || !guestDetails.moveInDate) {
      toast.warning('Please fill in your full name, phone number, and move-in date.');
      return;
    }

    setIsProcessing(true);
    // Simulate token payment intent
    setTimeout(() => {
      setIsProcessing(false);
      setCurrentStep(3);
      toast.success(`₹${guestDetails.tokenAmount} Token payment received! Unit reserved.`);
    }, 1200);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white">
        <div className="text-center space-y-3">
          <div className="w-10 h-10 border-2 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-xs font-semibold text-slate-400">Loading reservation portal...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-800 pb-16">
      {/* Top Header */}
      <header className="sticky top-0 z-30 bg-slate-900/90 backdrop-blur-md border-b border-slate-800 text-white">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <Link href="/explore" className="flex items-center gap-2 text-slate-300 hover:text-white transition-colors text-xs font-semibold">
            <ArrowLeft className="w-4 h-4" /> Back to Explore
          </Link>
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-blue-600 text-white flex items-center justify-center font-bold text-xs">
              S
            </div>
            <span className="font-extrabold text-sm tracking-tight">
              Stay<span className="text-blue-500">File</span>
            </span>
            <Badge variant="purple">Token Reservation</Badge>
          </div>
          <Link href="/login" className="text-xs text-slate-400 hover:text-white transition-colors">
            Resident Login
          </Link>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-3xl mx-auto px-4 sm:px-6 pt-8 space-y-6">
        {/* Step Indicator */}
        <div className="grid grid-cols-3 gap-3 text-center text-xs">
          {[
            { step: 1, label: 'Choose Room / Bed' },
            { step: 2, label: 'Guest Info & Token' },
            { step: 3, label: 'Reservation Confirmed' },
          ].map((s) => (
            <div
              key={s.step}
              className={`p-3 rounded-2xl border transition-all ${
                currentStep === s.step
                  ? 'bg-blue-600/10 border-blue-500 text-blue-400 font-bold'
                  : currentStep > s.step
                  ? 'bg-emerald-500/10 border-emerald-500/40 text-emerald-400 font-semibold'
                  : 'bg-slate-900 border-slate-800 text-slate-500'
              }`}
            >
              <div className="text-[10px] uppercase tracking-wider text-slate-400">Step 0{s.step}</div>
              <div className="truncate">{s.label}</div>
            </div>
          ))}
        </div>

        {/* Property Highlights Card */}
        {property && (
          <div className="p-5 rounded-3xl bg-slate-900 border border-slate-800 text-white flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <Badge variant="purple">{property.type}</Badge>
                <span className="text-xs text-emerald-400 font-semibold flex items-center gap-1">
                  <ShieldCheck className="w-3.5 h-3.5" /> Verified Listing
                </span>
              </div>
              <h1 className="text-xl font-black text-white">{property.name}</h1>
              <p className="text-xs text-slate-400 flex items-center gap-1">
                <MapPin className="w-3.5 h-3.5 text-slate-500" />
                {property.address}, {property.city}
              </p>
            </div>
            <div className="p-3 rounded-2xl bg-slate-800/80 border border-slate-700/80 text-right">
              <span className="text-[10px] uppercase tracking-wider text-slate-400 block">Holding Token</span>
              <span className="text-lg font-black text-emerald-400">₹2,000</span>
              <span className="text-[10px] text-slate-400 block">Deducted from 1st month rent</span>
            </div>
          </div>
        )}

        {/* STEP 1: SELECT UNIT */}
        {currentStep === 1 && (
          <div className="p-6 sm:p-8 rounded-3xl bg-white border border-slate-200 shadow-xl space-y-6">
            <div>
              <h2 className="text-lg font-black text-slate-900">Select Your Preferred Room / Bed</h2>
              <p className="text-xs text-slate-500 mt-1">
                Pick from currently available inventory. Holding token reserves your chosen spot exclusively for 48 hours.
              </p>
            </div>

            <div className="space-y-3">
              {units.map((unit) => {
                const isSelected = selectedUnit?.id === unit.id;
                return (
                  <div
                    key={unit.id}
                    onClick={() => setSelectedUnit(unit)}
                    className={`p-4 rounded-2xl border transition-all cursor-pointer flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 ${
                      isSelected
                        ? 'border-blue-600 bg-blue-50/60 shadow-md ring-2 ring-blue-500/20'
                        : 'border-slate-200 hover:border-slate-300 bg-white'
                    }`}
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-extrabold text-sm text-slate-900">{unit.unitNumber}</span>
                        <Badge variant={unit.sharingType === 'SINGLE' ? 'purple' : 'info'}>
                          {unit.sharingType} SHARING
                        </Badge>
                      </div>
                      <p className="text-xs text-slate-500">
                        Floor {unit.floorNumber} {unit.notes && `• ${unit.notes}`}
                      </p>
                    </div>

                    <div className="text-left sm:text-right">
                      <div className="text-base font-black text-slate-900">
                        {formatCurrency(unit.monthlyRent)}
                        <span className="text-xs font-normal text-slate-500"> / month</span>
                      </div>
                      <span className="text-[11px] text-slate-500 block">
                        Deposit: {formatCurrency(unit.securityDeposit)}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="flex justify-end pt-4 border-t border-slate-100">
              <Button
                className="font-bold px-6 shadow-md shadow-blue-500/20"
                disabled={!selectedUnit}
                onClick={() => setCurrentStep(2)}
              >
                Continue to Guest Details ➔
              </Button>
            </div>
          </div>
        )}

        {/* STEP 2: GUEST DETAILS & TOKEN PAYMENT */}
        {currentStep === 2 && selectedUnit && (
          <form
            onSubmit={handlePayToken}
            className="p-6 sm:p-8 rounded-3xl bg-white border border-slate-200 shadow-xl space-y-6"
          >
            <div>
              <h2 className="text-lg font-black text-slate-900">Guest Information & Reservation Advance</h2>
              <p className="text-xs text-slate-500 mt-1">
                Zero paperwork needed right now. Enter your contact info to hold <strong>{selectedUnit.unitNumber}</strong>.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Full Legal Name"
                placeholder="e.g. Ankit Sharma"
                value={guestDetails.fullName}
                onChange={(e) => setGuestDetails({ ...guestDetails, fullName: e.target.value })}
                required
              />
              <Input
                label="Mobile Number (WhatsApp)"
                type="tel"
                placeholder="+91 98765 43210"
                value={guestDetails.phone}
                onChange={(e) => setGuestDetails({ ...guestDetails, phone: e.target.value })}
                required
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Email Address (For Invoices & Receipt)"
                type="email"
                placeholder="ankit@example.com"
                value={guestDetails.email}
                onChange={(e) => setGuestDetails({ ...guestDetails, email: e.target.value })}
                required
              />
              <Input
                label="Target Move-In Date"
                type="date"
                value={guestDetails.moveInDate}
                onChange={(e) => setGuestDetails({ ...guestDetails, moveInDate: e.target.value })}
                required
              />
            </div>

            {/* Financial Summary */}
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2 text-xs">
              <div className="flex justify-between text-slate-600">
                <span>Selected Unit:</span>
                <span className="font-bold text-slate-900">{selectedUnit.unitNumber} ({property?.name})</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Monthly Rent:</span>
                <span className="font-bold text-slate-900">{formatCurrency(selectedUnit.monthlyRent)} / mo</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Security Deposit:</span>
                <span className="font-bold text-slate-900">{formatCurrency(selectedUnit.securityDeposit)}</span>
              </div>
              <div className="pt-2 border-t border-slate-200 flex justify-between items-center">
                <div>
                  <span className="font-bold text-slate-900 block">Token Advance Payable Now:</span>
                  <span className="text-[11px] text-slate-500">Locks unit for 48 hours & credited to your invoice</span>
                </div>
                <span className="text-xl font-black text-emerald-600">₹{guestDetails.tokenAmount}</span>
              </div>
            </div>

            <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl text-xs text-blue-900 flex items-start gap-2">
              <Sparkles className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
              <span>
                <strong>100% Secure:</strong> Mandatory identity verification (DigiLocker / Masked Aadhaar) will be completed online before room key handover.
              </span>
            </div>

            <div className="flex justify-between items-center pt-2">
              <Button type="button" variant="outline" onClick={() => setCurrentStep(1)}>
                <ArrowLeft className="w-4 h-4 mr-1" /> Change Unit
              </Button>
              <Button type="submit" isLoading={isProcessing} className="font-bold px-6 shadow-md shadow-blue-500/20">
                Pay ₹{guestDetails.tokenAmount} via UPI / Card ➔
              </Button>
            </div>
          </form>
        )}

        {/* STEP 3: SUCCESS & NEXT STEP TO KYC */}
        {currentStep === 3 && selectedUnit && (
          <div className="p-8 sm:p-10 rounded-3xl bg-white border border-slate-200 shadow-xl text-center space-y-6">
            <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 mx-auto flex items-center justify-center font-bold text-3xl">
              ✓
            </div>

            <div className="space-y-1">
              <h2 className="text-2xl font-black text-slate-900">Bed Reserved Exclusively for You!</h2>
              <p className="text-sm text-slate-500">
                Unit <strong>{selectedUnit.unitNumber}</strong> at <strong>{property?.name}</strong> is locked.
              </p>
            </div>

            {/* Receipt Summary */}
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 max-w-md mx-auto text-left text-xs space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-500">Booking Reference:</span>
                <span className="font-mono font-bold text-slate-800">#BK-{Math.floor(10000 + Math.random() * 90000)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Token Amount Paid:</span>
                <span className="font-bold text-emerald-600">₹{guestDetails.tokenAmount}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Expected Move-in:</span>
                <span className="font-bold text-slate-800">{guestDetails.moveInDate}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Move-in Balance Remaining:</span>
                <span className="font-bold text-slate-900">
                  {formatCurrency(selectedUnit.monthlyRent + selectedUnit.securityDeposit - guestDetails.tokenAmount)}
                </span>
              </div>
            </div>

            {/* Action Prompt */}
            <div className="p-4 bg-amber-50 border border-amber-200 rounded-2xl max-w-md mx-auto text-left text-xs space-y-1">
              <span className="font-bold text-amber-900 flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4 text-amber-600" /> Final Step: Mandatory KYC & Police Verification
              </span>
              <p className="text-[11px] text-amber-800 leading-relaxed">
                State tenancy regulations require online identity verification (DigiLocker Aadhaar OTP or PAN) and digital agreement e-sign before key handover.
              </p>
            </div>

            <div className="space-y-3 max-w-md mx-auto pt-2">
              <Button
                className="w-full font-bold bg-blue-600 hover:bg-blue-700 text-white shadow-lg shadow-blue-500/25 py-3"
                onClick={() =>
                  router.push(
                    `/kyc/tenant?propertyId=${propertyId}&unitId=${selectedUnit.id}&tokenPaid=${guestDetails.tokenAmount}&fullName=${encodeURIComponent(
                      guestDetails.fullName
                    )}&phone=${encodeURIComponent(guestDetails.phone)}&email=${encodeURIComponent(guestDetails.email)}`
                  )
                }
              >
                Proceed to Online KYC & E-Sign ➔
              </Button>
              <Link
                href="/explore"
                className="block text-xs font-semibold text-slate-500 hover:text-slate-700 transition-colors"
              >
                Return to Explore Marketplace
              </Link>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
