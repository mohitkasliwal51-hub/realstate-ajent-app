'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  Building2,
  Users,
  FileText,
  Receipt,
  Settings,
  LayoutDashboard,
  LogOut,
  ExternalLink,
  Wrench,
  UserCheck,
  Briefcase,
  Zap,
  Wallet,
  FileSpreadsheet,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { isBrokerMode, getOrgMode } from '@/lib/orgMode';
import { cn } from '@/lib/utils';

export const Sidebar: React.FC = () => {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const brokerMode = isBrokerMode(user);
  const orgMode = getOrgMode(user);

  const navItems = [
    { name: 'Dashboard', href: '/app', icon: LayoutDashboard },
    { name: 'Properties', href: '/app/properties', icon: Building2 },
    ...(brokerMode ? [{ name: 'Landlords', href: '/app/landlords', icon: Briefcase }] : []),
    { name: 'Tenants', href: '/app/tenants', icon: Users },
    { name: 'Leases', href: '/app/leases', icon: FileText },
    { name: 'Invoices', href: '/app/invoices', icon: FileSpreadsheet },
    { name: 'Receipts', href: '/app/receipts', icon: Receipt },
    ...(brokerMode ? [{ name: 'Landlord Payouts', href: '/app/payouts', icon: Wallet }] : []),
    { name: 'Meter Readings', href: '/app/meter-readings', icon: Zap },
    { name: 'Tickets', href: '/app/tickets', icon: Wrench },
    { name: 'CRM Leads', href: '/app/leads', icon: UserCheck },
    { name: 'Settings', href: '/app/settings', icon: Settings },
  ];

  return (
    <aside className="w-64 bg-slate-900 text-slate-300 flex flex-col min-h-screen border-r border-slate-800 shrink-0">
      {/* Brand Header */}
      <div className="h-16 flex items-center justify-between px-6 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-blue-600 flex items-center justify-center text-white font-bold text-lg shadow-md shadow-blue-500/20">
            S
          </div>
          <div>
            <span className="font-bold text-white tracking-tight text-lg">StayFile</span>
            <span className="block text-[10px] text-blue-400 font-semibold tracking-wider uppercase">
              Property OS
            </span>
          </div>
        </div>
        <span className="px-2 py-0.5 text-[9px] font-extrabold uppercase rounded bg-slate-800 text-slate-300 border border-slate-700">
          {orgMode}
        </span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-4 py-4 space-y-1 overflow-y-auto">
        <div className="px-3 pb-2 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
          Management
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href || (item.href !== '/app' && pathname.startsWith(item.href));

          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all group',
                isActive
                  ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/20 font-semibold'
                  : 'hover:bg-slate-800/60 text-slate-400 hover:text-slate-200'
              )}
            >
              <Icon
                className={cn(
                  'w-4 h-4 transition-transform group-hover:scale-105',
                  isActive ? 'text-white' : 'text-slate-400 group-hover:text-slate-200'
                )}
              />
              {item.name}
            </Link>
          );
        })}

        {/* Public Showcase Link */}
        <div className="pt-4 px-3 pb-2 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
          Public Portal
        </div>
        <Link
          href={`/p/${user?.organizationSlug || 'default-org'}`}
          target="_blank"
          className="flex items-center gap-3 px-3 py-2 rounded-xl text-sm font-medium text-slate-400 hover:bg-slate-800/60 hover:text-slate-200 transition-all"
        >
          <ExternalLink className="w-4 h-4 text-slate-400" />
          Public Showcase
        </Link>
      </nav>

      {/* User Footer */}
      <div className="p-4 border-t border-slate-800 bg-slate-950/40">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center text-slate-200 font-semibold text-xs border border-slate-700">
              {user?.fullName?.charAt(0) || 'U'}
            </div>
            <div className="min-w-0">
              <p className="text-xs font-semibold text-white truncate">
                {user?.fullName || 'Manager'}
              </p>
              <p className="text-[10px] text-slate-400 truncate">
                {user?.role || 'PROPERTY_MANAGER'}
              </p>
            </div>
          </div>
          <button
            onClick={logout}
            title="Logout"
            className="p-1.5 text-slate-400 hover:text-red-400 hover:bg-slate-800 rounded-lg transition-colors"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
};
