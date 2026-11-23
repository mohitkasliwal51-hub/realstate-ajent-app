'use client';

import React, { useState, useEffect, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { User, Building2, ShieldCheck, CheckCircle2 } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { authApi } from '@/lib/api/authApi';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';

function getErrorMessage(error: unknown, fallback: string): string {
  if (error && typeof error === 'object' && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return fallback;
}

function RegisterForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();

  const [roleMode, setRoleMode] = useState<'TENANT' | 'LANDLORD'>('TENANT');

  useEffect(() => {
    const roleParam = searchParams.get('role');
    if (roleParam === 'LANDLORD' || roleParam === 'OWNER') {
      setRoleMode('LANDLORD');
    } else if (roleParam === 'TENANT') {
      setRoleMode('TENANT');
    }
  }, [searchParams]);

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    password: '',
    organizationName: '',
    organizationType: 'OWNER' as 'OWNER' | 'BROKERAGE' | 'HYBRID',
  });

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);

    try {
      const payload = {
        fullName: formData.fullName,
        email: formData.email,
        phone: formData.phone,
        password: formData.password,
        organizationName:
          roleMode === 'LANDLORD'
            ? formData.organizationName
            : `${formData.fullName} (Resident)`,
        organizationType: roleMode === 'LANDLORD' ? formData.organizationType : 'OWNER',
      };

      const data = await authApi.register(payload);
      login(data.token, data.user);

      if (roleMode === 'TENANT') {
        router.push('/explore');
      } else {
        router.push('/app');
      }
    } catch (err: unknown) {
      setError(getErrorMessage(err, 'Registration failed. Please check details and try again.'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <article className="w-full max-w-md bg-white rounded-3xl shadow-2xl p-6 sm:p-8 border border-slate-100">
      {/* Brand & Title */}
      <header className="flex flex-col items-center text-center mb-5">
        <div className="w-11 h-11 rounded-2xl bg-blue-600 flex items-center justify-center text-white font-black text-xl mb-2.5 shadow-lg shadow-blue-500/25">
          S
        </div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Create Free Account</h1>
        <p className="text-xs text-slate-500 mt-1">
          Quick frictionless onboarding. Zero mandatory verification upfront.
        </p>
      </header>

      {/* Role Selector Tabs */}
      <fieldset className="grid grid-cols-2 gap-2 p-1 bg-slate-100 rounded-2xl mb-6">
        <legend className="sr-only">Choose account type</legend>
        <button
          type="button"
          aria-pressed={roleMode === 'TENANT'}
          onClick={() => setRoleMode('TENANT')}
          className={`flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs font-bold transition-all ${
            roleMode === 'TENANT'
              ? 'bg-white text-blue-700 shadow-sm'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          <User className="w-3.5 h-3.5" />
          <span>I am a Tenant</span>
        </button>

        <button
          type="button"
          aria-pressed={roleMode === 'LANDLORD'}
          onClick={() => setRoleMode('LANDLORD')}
          className={`flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs font-bold transition-all ${
            roleMode === 'LANDLORD'
              ? 'bg-white text-blue-700 shadow-sm'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          <Building2 className="w-3.5 h-3.5" />
          <span>Owner / Agency</span>
        </button>
      </fieldset>

      {error && (
        <div role="alert" aria-live="polite" className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl font-medium">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-3.5" aria-labelledby="register-title">
        <h2 id="register-title" className="sr-only">Create account</h2>
        {/* Owner / Broker specific fields */}
        {roleMode === 'LANDLORD' && (
          <>
            <Input
              label="Business / Property Brand Name"
              placeholder="e.g. Acme Residencies or Royal Stays"
              value={formData.organizationName}
              onChange={(e) => setFormData({ ...formData, organizationName: e.target.value })}
              required
            />

            <div className="flex flex-col gap-1.5">
              <label htmlFor="operating-model" className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                Operating Model
              </label>
              <select
                id="operating-model"
                name="organizationType"
                value={formData.organizationType}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    organizationType: e.target.value as 'OWNER' | 'BROKERAGE' | 'HYBRID',
                  })
                }
                className="w-full h-10 px-3 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 font-medium"
              >
                <option value="OWNER">Direct Landlord (Self-owned Properties / PGs)</option>
                <option value="BROKERAGE">Brokerage / Agency (Managing 3rd-party landlords)</option>
                <option value="HYBRID">PG Operator / Multi-Property Co-Living</option>
              </select>
            </div>
          </>
        )}

        {/* Common basic fields */}
        <Input
          label="Your Full Name"
          placeholder="e.g. Ankit Sharma"
          value={formData.fullName}
          onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
          required
        />

        <Input
          label="Email Address"
          type="email"
          placeholder="ankit@example.com"
          value={formData.email}
          onChange={(e) => setFormData({ ...formData, email: e.target.value })}
          required
        />

        <Input
          label="Phone Number"
          type="tel"
          placeholder="+91 98765 43210"
          value={formData.phone}
          onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
          required
        />

        <Input
          label="Password"
          type="password"
          placeholder="••••••••"
          value={formData.password}
          onChange={(e) => setFormData({ ...formData, password: e.target.value })}
          required
        />

        <div className="p-3 bg-slate-50 border border-slate-200/80 rounded-xl text-[11px] text-slate-500">
          {roleMode === 'TENANT' ? (
            <p>
              💡 <strong>Instant Access:</strong> Browse verified listings immediately. Mandatory KYC & police verification will only be required when finalizing your lease.
            </p>
          ) : (
            <p>
              💡 <strong>Owner Verification:</strong> You can configure settings immediately. PAN & Bank Account verification will be required before publishing listings.
            </p>
          )}
        </div>

        <Button type="submit" className="w-full font-bold shadow-md shadow-blue-500/20" isLoading={isLoading}>
          {roleMode === 'TENANT' ? 'Get Started as Resident ➔' : 'Create Owner Portal ➔'}
        </Button>
      </form>

      <footer className="mt-5 text-center text-xs text-slate-500">
        Already have an account?{' '}
        <Link href="/login" className="font-bold text-blue-600 hover:text-blue-700">
          Sign In
        </Link>
      </footer>
    </article>
  );
}

export default function RegisterPage() {
  return (
    <main className="min-h-screen flex items-center justify-center bg-slate-950 p-4">
      <Suspense fallback={<div className="text-white text-xs">Loading registration...</div>}>
        <RegisterForm />
      </Suspense>
    </main>
  );
}
