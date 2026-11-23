'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  Building2,
  Users,
  FileText,
  FileSpreadsheet,
  Plus,
  ArrowRight,
  Sparkles,
  TrendingUp,
  Wrench,
  CheckCircle2,
  Clock,
  AlertCircle,
  ExternalLink,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { propertyApi, Property } from '@/lib/api/propertyApi';
import { leaseApi, Lease } from '@/lib/api/leaseApi';
import { tenantApi, Tenant } from '@/lib/api/tenantApi';
import { invoiceApi, Invoice } from '@/lib/api/invoiceApi';
import { ticketApi, MaintenanceTicket } from '@/lib/api/ticketApi';
import { StatCard } from '@/components/ui/StatCard';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { formatCurrency } from '@/lib/utils';

export default function DashboardPage() {
  const { user, isLoading: isAuthLoading } = useAuth();
  const router = useRouter();

  const [properties, setProperties] = useState<Property[]>([]);
  const [leases, setLeases] = useState<Lease[]>([]);
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [tickets, setTickets] = useState<MaintenanceTicket[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (isAuthLoading) return;

    if (user?.role === 'TENANT') {
      router.replace('/app/tenant');
      return;
    }
    if (!user?.organizationId) {
      setIsLoading(false);
      return;
    }

    const fetchData = async () => {
      setIsLoading(true);
      try {
        const [propsRes, leasesRes, tenantsRes, invoicesRes, ticketsRes] = await Promise.allSettled([
          propertyApi.getProperties(),
          leaseApi.getLeases(),
          tenantApi.getTenants(),
          invoiceApi.getInvoices(),
          ticketApi.getTicketsByOrganization(),
        ]);

        if (propsRes.status === 'fulfilled') setProperties(propsRes.value || []);
        if (leasesRes.status === 'fulfilled') setLeases(leasesRes.value || []);
        if (tenantsRes.status === 'fulfilled') setTenants(tenantsRes.value || []);
        if (invoicesRes.status === 'fulfilled') setInvoices(invoicesRes.value || []);
        if (ticketsRes.status === 'fulfilled') setTickets(ticketsRes.value || []);
      } catch (err) {
        console.error('Failed to load dashboard metrics', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [user?.organizationId, user?.role, isAuthLoading, router]);

  // Derived metrics
  const activeLeases = leases.filter((l) => l.status === 'ACTIVE');
  const pendingLeases = leases.filter((l) => l.status === 'DRAFT' || l.status === 'PENDING_ESIGN');

  const totalInvoiced = invoices.reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);
  const totalCollected = invoices.reduce((sum, inv) => sum + (inv.paidAmount || 0), 0);
  const collectionRate = totalInvoiced > 0 ? Math.round((totalCollected / totalInvoiced) * 100) : 100;

  const openTickets = tickets.filter((t) => t.status === 'OPEN' || t.status === 'IN_PROGRESS');
  const urgentTickets = openTickets.filter((t) => t.priority === 'HIGH' || t.priority === 'URGENT');

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-300">
      {/* Top Welcome Banner */}
      <section aria-labelledby="dashboard-welcome" className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-slate-900 via-blue-950 to-indigo-950 p-6 sm:p-8 text-white shadow-xl border border-slate-800">
        <div className="absolute -right-8 -bottom-8 w-64 h-64 bg-blue-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="space-y-2 max-w-xl">
            <div className="inline-flex items-center gap-2 px-3 py-1 bg-white/10 backdrop-blur-md border border-white/15 rounded-full text-xs font-semibold text-blue-200">
              <Sparkles className="w-3.5 h-3.5 text-blue-300" />
              <span>StayFile Property OS &bull; Portfolio Active</span>
            </div>
            <h1 id="dashboard-welcome" className="text-2xl sm:text-3xl font-black tracking-tight text-white">
              Welcome back, {user?.fullName || 'Manager'}
            </h1>
            <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
              Real-time real estate management: unit inventory, e-signed leases, automated rent invoicing, and tenant requests.
            </p>
          </div>

          <div className="relative z-10 flex flex-wrap items-center gap-3">
            <Link href="/app/properties">
              <Button variant="primary">
                <Plus className="w-4 h-4 mr-1.5" /> Add Property
              </Button>
            </Link>
            <Link href="/app/leases">
              <Button variant="outline" className="bg-white/10 border-white/20 text-white hover:bg-white/20">
                Create Lease
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Primary Stat Cards Grid */}
      <section aria-label="Portfolio overview" className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5">
        <StatCard
          title="Properties"
          value={isLoading ? '...' : properties.length}
          icon={Building2}
          iconColor="text-blue-600"
          iconBg="bg-blue-50"
          trend="Active"
          trendDirection="up"
          trendLabel="Operational Buildings"
        />

        <StatCard
          title="Active Leases"
          value={isLoading ? '...' : activeLeases.length}
          icon={Users}
          iconColor="text-emerald-600"
          iconBg="bg-emerald-50"
          trend={`${pendingLeases.length} Pending`}
          trendDirection={pendingLeases.length > 0 ? 'neutral' : 'up'}
          trendLabel={`${tenants.length} Total Tenants`}
        />

        <StatCard
          title="Rent Collection"
          value={isLoading ? '...' : `${collectionRate}%`}
          icon={FileSpreadsheet}
          iconColor="text-indigo-600"
          iconBg="bg-indigo-50"
          progress={isLoading ? undefined : collectionRate}
          progressColor="bg-indigo-600"
          trend={isLoading ? undefined : formatCurrency(totalCollected)}
          trendDirection={collectionRate >= 80 ? 'up' : 'down'}
          trendLabel={isLoading ? undefined : `of ${formatCurrency(totalInvoiced)}`}
        />

        <StatCard
          title="Open Tickets"
          value={isLoading ? '...' : openTickets.length}
          icon={Wrench}
          iconColor={urgentTickets.length > 0 ? 'text-rose-600' : 'text-amber-600'}
          iconBg={urgentTickets.length > 0 ? 'bg-rose-50' : 'bg-amber-50'}
          trend={urgentTickets.length > 0 ? `${urgentTickets.length} Urgent` : 'Normal'}
          trendDirection={urgentTickets.length > 0 ? 'down' : 'neutral'}
          trendLabel={`${tickets.length - openTickets.length} Resolved`}
        />
      </section>

      {/* Operational Split: Recent Invoices & Maintenance Feed */}
      <section aria-label="Recent operational activity" className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Invoices Widget */}
        <article className="bg-white rounded-2xl border border-slate-200/80 shadow-xs overflow-hidden flex flex-col justify-between">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
                <FileSpreadsheet className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-900">Billing & Invoices</h3>
                <p className="text-[11px] text-slate-500">Recent customer invoices and payment states</p>
              </div>
            </div>
            <Link
              href="/app/invoices"
              className="text-xs font-semibold text-blue-600 hover:text-blue-700 flex items-center gap-1 group"
            >
              View all <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
            </Link>
          </div>

          <div className="p-4 flex-1">
            {isLoading ? (
              <div className="space-y-3 py-4">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-12 bg-slate-100 rounded-xl animate-pulse" />
                ))}
              </div>
            ) : invoices.length === 0 ? (
              <div className="py-8 px-4 text-center rounded-xl bg-slate-50/70 border border-dashed border-slate-200 my-1">
                <FileSpreadsheet className="w-8 h-8 mx-auto text-slate-300 mb-2" />
                <p className="text-xs font-semibold text-slate-700">No invoices issued yet</p>
                <p className="text-[11px] text-slate-400 mt-0.5 mb-3">Generate invoices for active tenant leases</p>
                <Link href="/app/invoices">
                  <Button variant="outline" size="sm" className="text-xs h-7 bg-white">
                    <Plus className="w-3 h-3 mr-1" /> New Invoice
                  </Button>
                </Link>
              </div>
            ) : (
              <ul className="divide-y divide-slate-100">
                {invoices.slice(0, 4).map((inv) => (
                  <li key={inv.id} className="py-3 flex items-center justify-between gap-3 text-xs">
                    <div className="min-w-0">
                      <div className="font-bold text-slate-900 truncate">
                        {inv.invoiceNumber || 'INV-DRAFT'}
                      </div>
                      <div className="text-[11px] text-slate-500">Due: {inv.dueDate || 'N/A'}</div>
                    </div>
                    <div className="text-right shrink-0">
                      <div className="font-bold text-slate-900">{formatCurrency(inv.totalAmount)}</div>
                      <Badge
                        variant={
                          inv.status === 'PAID'
                            ? 'success'
                            : inv.status === 'OVERDUE'
                            ? 'danger'
                            : 'warning'
                        }
                        dot
                      >
                        {inv.status}
                      </Badge>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </article>

        {/* Maintenance Tickets Widget */}
        <article className="bg-white rounded-2xl border border-slate-200/80 shadow-xs overflow-hidden flex flex-col justify-between">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center font-bold">
                <Wrench className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-900">Maintenance & Tickets</h3>
                <p className="text-[11px] text-slate-500">Operational service requests from tenants</p>
              </div>
            </div>
            <Link
              href="/app/tickets"
              className="text-xs font-semibold text-blue-600 hover:text-blue-700 flex items-center gap-1 group"
            >
              View all <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
            </Link>
          </div>

          <div className="p-4 flex-1">
            {isLoading ? (
              <div className="space-y-3 py-4">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-12 bg-slate-100 rounded-xl animate-pulse" />
                ))}
              </div>
            ) : tickets.length === 0 ? (
              <div className="py-8 px-4 text-center rounded-xl bg-slate-50/70 border border-dashed border-slate-200 my-1">
                <CheckCircle2 className="w-8 h-8 mx-auto text-emerald-500/80 mb-2" />
                <p className="text-xs font-semibold text-slate-700">All maintenance tickets resolved</p>
                <p className="text-[11px] text-slate-400 mt-0.5">No pending tenant complaints or repair requests</p>
              </div>
            ) : (
              <ul className="divide-y divide-slate-100">
                {tickets.slice(0, 4).map((ticket) => (
                  <li key={ticket.id} className="py-3 flex items-center justify-between gap-3 text-xs">
                    <div className="min-w-0">
                      <div className="font-bold text-slate-900 truncate">{ticket.title}</div>
                      <div className="text-[11px] text-slate-500">
                        {ticket.category} &bull; {ticket.tenantName || 'Resident'}
                      </div>
                    </div>
                    <div className="text-right shrink-0">
                      <Badge
                        variant={
                          ticket.status === 'RESOLVED' || ticket.status === 'CLOSED'
                            ? 'success'
                            : ticket.priority === 'URGENT'
                            ? 'danger'
                            : 'warning'
                        }
                      >
                        {ticket.status}
                      </Badge>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </article>
      </section>

      {/* Quick Action Navigation Grid */}
      <nav aria-labelledby="quick-actions-heading" className="space-y-3">
        <h2 id="quick-actions-heading" className="text-sm font-bold text-slate-900 uppercase tracking-wider">Quick Action Workflows</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Link
            href="/app/properties"
            className="group bg-white p-5 rounded-2xl border border-slate-200/80 hover:border-blue-500 hover:shadow-md transition-all space-y-2"
          >
            <div className="w-9 h-9 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-black text-sm">
              1
            </div>
            <h3 className="font-bold text-sm text-slate-900 group-hover:text-blue-600 flex items-center justify-between">
              Properties & Units
              <ArrowRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform" />
            </h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Manage PG, Hostel, Apartment, and Commercial building unit availability and amenities.
            </p>
          </Link>

          <Link
            href="/app/tenants"
            className="group bg-white p-5 rounded-2xl border border-slate-200/80 hover:border-purple-500 hover:shadow-md transition-all space-y-2"
          >
            <div className="w-9 h-9 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center font-black text-sm">
              2
            </div>
            <h3 className="font-bold text-sm text-slate-900 group-hover:text-purple-600 flex items-center justify-between">
              Tenant Onboarding
              <ArrowRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform" />
            </h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Collect contact details, permanent residence address, emergency contacts, and KYC.
            </p>
          </Link>

          <Link
            href="/app/leases"
            className="group bg-white p-5 rounded-2xl border border-slate-200/80 hover:border-emerald-500 hover:shadow-md transition-all space-y-2"
          >
            <div className="w-9 h-9 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-black text-sm">
              3
            </div>
            <h3 className="font-bold text-sm text-slate-900 group-hover:text-emerald-600 flex items-center justify-between">
              Leases & Agreements
              <ArrowRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform" />
            </h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Generate standardized rent agreements, initiate e-signatures, and issue official receipts.
            </p>
          </Link>
        </div>
      </nav>
    </div>
  );
}
