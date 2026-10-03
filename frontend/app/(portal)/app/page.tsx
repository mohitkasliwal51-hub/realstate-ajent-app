'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  Building2,
  Users,
  FileText,
  Receipt as ReceiptIcon,
  TrendingUp,
  Plus,
  ArrowRight,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { propertyApi, Property } from '@/lib/api/propertyApi';
import { leaseApi, Lease } from '@/lib/api/leaseApi';
import { tenantApi, Tenant } from '@/lib/api/tenantApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { formatCurrency } from '@/lib/utils';

export default function DashboardPage() {
  const { user } = useAuth();
  const [properties, setProperties] = useState<Property[]>([]);
  const [leases, setLeases] = useState<Lease[]>([]);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!user?.organizationId) return;

    const fetchData = async () => {
      setIsLoading(true);
      try {
        const [propsRes, leasesRes, tenantsRes] = await Promise.allSettled([
          propertyApi.getProperties(),
          leaseApi.getLeases(),
          tenantApi.getTenants(),
        ]);

        if (propsRes.status === 'fulfilled') setProperties(propsRes.value || []);
        if (leasesRes.status === 'fulfilled') setLeases(leasesRes.value || []);
        if (tenantsRes.status === 'fulfilled') setTenants(tenantsRes.value || []);
      } catch (err) {
        console.error('Failed to load dashboard metrics', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [user?.organizationId]);

  const activeLeasesCount = leases.filter((l) => l.status === 'ACTIVE').length;
  const draftLeasesCount = leases.filter((l) => l.status === 'DRAFT' || l.status === 'PENDING_ESIGN').length;

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="bg-gradient-to-r from-blue-900 via-blue-800 to-indigo-900 rounded-2xl p-6 md:p-8 text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-6 relative overflow-hidden">
        <div className="relative z-10 space-y-2 max-w-xl">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-blue-500/20 border border-blue-400/30 rounded-full text-xs font-semibold text-blue-200 backdrop-blur-sm">
            <Sparkles className="w-3.5 h-3.5" />
            <span>StayFile Property OS v2.0</span>
          </div>
          <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight">
            Welcome back, {user?.fullName || 'Manager'}
          </h1>
          <p className="text-sm text-blue-100/90 leading-relaxed">
            Manage your real estate portfolio, tenant onboarding, automated rent agreements, and receipts in real time.
          </p>
        </div>

        <div className="relative z-10 flex items-center gap-3">
          <Link href="/app/properties">
            <Button variant="primary" className="shadow-lg shadow-blue-500/30">
              <Plus className="w-4 h-4 mr-2" /> Add Property
            </Button>
          </Link>
          <Link href="/app/leases">
            <Button variant="outline" className="bg-white/10 border-white/20 text-white hover:bg-white/20">
              Create Lease
            </Button>
          </Link>
        </div>
      </div>

      {/* Stats Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Properties</p>
            <p className="text-2xl font-bold text-slate-900">{isLoading ? '...' : properties.length}</p>
            <p className="text-[11px] text-emerald-600 font-medium flex items-center gap-1">
              <TrendingUp className="w-3 h-3" /> Active Locations
            </p>
          </div>
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
            <Building2 className="w-6 h-6" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Tenants</p>
            <p className="text-2xl font-bold text-slate-900">{isLoading ? '...' : tenants.length}</p>
            <p className="text-[11px] text-slate-500">Verified KYC</p>
          </div>
          <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
            <Users className="w-6 h-6" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Active Leases</p>
            <p className="text-2xl font-bold text-slate-900">{isLoading ? '...' : activeLeasesCount}</p>
            <p className="text-[11px] text-amber-600 font-medium">{draftLeasesCount} Pending/Draft</p>
          </div>
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <FileText className="w-6 h-6" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">System Status</p>
            <Badge variant="success">Operational</Badge>
            <p className="text-[11px] text-slate-500">Spring Boot REST API connected</p>
          </div>
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
            <ReceiptIcon className="w-6 h-6" />
          </div>
        </div>
      </div>

      {/* Quick Action Navigation Grid */}
      <div className="space-y-4">
        <h2 className="text-lg font-bold text-slate-900">Workflow Short Cuts</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          <Link
            href="/app/properties"
            className="group bg-white p-6 rounded-2xl border border-slate-200 hover:border-blue-500 hover:shadow-md transition-all space-y-3"
          >
            <div className="w-10 h-10 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center font-bold">
              1
            </div>
            <div>
              <h3 className="font-bold text-slate-900 group-hover:text-blue-600 flex items-center justify-between">
                Properties & Units <ArrowRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform" />
              </h3>
              <p className="text-xs text-slate-500 mt-1">
                Configure PG, Hostel, Flat, or Commercial inventory and unit availability status.
              </p>
            </div>
          </Link>

          <Link
            href="/app/tenants"
            className="group bg-white p-6 rounded-2xl border border-slate-200 hover:border-purple-500 hover:shadow-md transition-all space-y-3"
          >
            <div className="w-10 h-10 rounded-xl bg-purple-100 text-purple-600 flex items-center justify-center font-bold">
              2
            </div>
            <div>
              <h3 className="font-bold text-slate-900 group-hover:text-purple-600 flex items-center justify-between">
                Tenant Onboarding <ArrowRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform" />
              </h3>
              <p className="text-xs text-slate-500 mt-1">
                Collect contact details, permanent address, ID proof, and KYC verification status.
              </p>
            </div>
          </Link>

          <Link
            href="/app/leases"
            className="group bg-white p-6 rounded-2xl border border-slate-200 hover:border-emerald-500 hover:shadow-md transition-all space-y-3"
          >
            <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center font-bold">
              3
            </div>
            <div>
              <h3 className="font-bold text-slate-900 group-hover:text-emerald-600 flex items-center justify-between">
                Lease Lifecycle & Receipts <ArrowRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform" />
              </h3>
              <p className="text-xs text-slate-500 mt-1">
                Execute lease lifecycle (Draft → Active), prevent overlaps, and issue rent receipts.
              </p>
            </div>
          </Link>
        </div>
      </div>
    </div>
  );
}
