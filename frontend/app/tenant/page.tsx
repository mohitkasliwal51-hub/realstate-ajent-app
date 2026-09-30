'use client';

import React from 'react';
import Link from 'next/link';
import { Home, FileText, Receipt as ReceiptIcon, ShieldCheck, User } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';

export default function TenantPortalPage() {
  const { user } = useAuth();

  return (
    <div className="min-h-screen bg-slate-900 text-white p-6 md:p-12">
      <div className="max-w-4xl mx-auto space-y-8">
        <header className="flex items-center justify-between border-b border-slate-800 pb-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-purple-600 flex items-center justify-center font-bold text-xl">
              T
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight">Tenant Self-Service Portal</h1>
              <p className="text-xs text-slate-400">Welcome, {user?.fullName || 'Tenant'}</p>
            </div>
          </div>
          <Link href="/app">
            <Button variant="outline" className="border-slate-700 text-slate-300 hover:bg-slate-800">
              Staff Portal
            </Button>
          </Link>
        </header>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-slate-800/80 p-6 rounded-2xl border border-slate-700 space-y-3">
            <div className="w-10 h-10 rounded-xl bg-purple-500/20 text-purple-400 flex items-center justify-center">
              <Home className="w-5 h-5" />
            </div>
            <h3 className="font-bold text-lg">My Residence</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              View your assigned unit, monthly rent schedule, and active occupancy status.
            </p>
          </div>

          <div className="bg-slate-800/80 p-6 rounded-2xl border border-slate-700 space-y-3">
            <div className="w-10 h-10 rounded-xl bg-blue-500/20 text-blue-400 flex items-center justify-center">
              <FileText className="w-5 h-5" />
            </div>
            <h3 className="font-bold text-lg">Lease Agreement</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Preview lease contract terms, e-signature history, and download PDF copies.
            </p>
          </div>

          <div className="bg-slate-800/80 p-6 rounded-2xl border border-slate-700 space-y-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
              <ReceiptIcon className="w-5 h-5" />
            </div>
            <h3 className="font-bold text-lg">Rent Receipts</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              Access your organization-verified payment receipts and payment reference logs.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
