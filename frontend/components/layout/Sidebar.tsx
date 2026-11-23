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
  X,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { isBrokerMode, getOrgMode } from '@/lib/orgMode';
import { cn } from '@/lib/utils';

export interface SidebarProps {
  isOpen?: boolean;
  onClose?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen = false, onClose }) => {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const brokerMode = isBrokerMode(user);
  const orgMode = getOrgMode(user);

  const isTenant = user?.role === 'TENANT';
  const isAdminOrPM = user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN' || user?.role === 'PROPERTY_MANAGER';

  const tenantNavItems = [
    { name: 'My Residence', href: '/app/tenant', icon: LayoutDashboard },
    { name: 'My Invoices', href: '/app/invoices', icon: FileSpreadsheet },
    { name: 'Payment Receipts', href: '/app/receipts', icon: Receipt },
    { name: 'Maintenance Tickets', href: '/app/tickets', icon: Wrench },
    { name: 'Meter Readings', href: '/app/meter-readings', icon: Zap },
  ];

  const staffNavItems = [
    { name: 'Dashboard', href: '/app', icon: LayoutDashboard },
    { name: 'Properties', href: '/app/properties', icon: Building2 },
    ...(brokerMode && isAdminOrPM ? [{ name: 'Landlords', href: '/app/landlords', icon: Briefcase }] : []),
    { name: 'Tenants', href: '/app/tenants', icon: Users },
    { name: 'Leases', href: '/app/leases', icon: FileText },
    { name: 'Invoices', href: '/app/invoices', icon: FileSpreadsheet },
    { name: 'Receipts', href: '/app/receipts', icon: Receipt },
    ...(brokerMode && isAdminOrPM ? [{ name: 'Landlord Payouts', href: '/app/payouts', icon: Wallet }] : []),
    { name: 'Meter Readings', href: '/app/meter-readings', icon: Zap },
    { name: 'Tickets', href: '/app/tickets', icon: Wrench },
    { name: 'CRM Leads', href: '/app/leads', icon: UserCheck },
    ...(isAdminOrPM ? [{ name: 'Settings', href: '/app/settings', icon: Settings }] : []),
  ];

  const navItems = isTenant ? tenantNavItems : staffNavItems;

  const NavContent = () => (
    <div className="flex flex-col h-full bg-slate-900 text-slate-300">
      {/* Brand Header */}
      <div className="h-16 flex items-center justify-between px-6 border-b border-slate-800 shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-white font-extrabold text-lg shadow-md shadow-blue-500/25">
            S
          </div>
          <div>
            <span className="font-extrabold text-white tracking-tight text-lg">StayFile</span>
            <span className="block text-[10px] text-blue-400 font-bold tracking-wider uppercase">
              {isTenant ? 'Tenant Portal' : 'Property OS'}
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span className="px-2 py-0.5 text-[9px] font-black uppercase rounded-md bg-slate-800 text-slate-300 border border-slate-700">
            {isTenant ? 'TENANT' : orgMode}
          </span>
          {onClose && (
            <button
              onClick={onClose}
              aria-label="Close navigation menu"
              className="md:hidden text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 px-4 py-4 space-y-1 overflow-y-auto scrollbar-thin">
        <h2 className="sr-only">{isTenant ? 'Tenant navigation' : 'Workspace navigation'}</h2>
        <div className="px-3 pb-2 text-[10px] font-bold text-slate-500 uppercase tracking-wider">
          {isTenant ? 'Tenant Hub' : 'Management Console'}
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive =
            pathname === item.href ||
            (item.href !== '/app' && item.href !== '/app/tenant' && pathname.startsWith(item.href));

          return (
            <Link
              key={item.name}
              href={item.href}
              onClick={() => onClose?.()}
              className={cn(
                'flex items-center justify-between px-3 py-2.5 rounded-xl text-xs sm:text-sm font-semibold transition-all group relative',
                isActive
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-600/25 font-bold'
                  : 'hover:bg-slate-800/70 text-slate-400 hover:text-slate-100'
              )}
            >
              <div className="flex items-center gap-3">
                <Icon
                  className={cn(
                    'w-4 h-4 transition-transform group-hover:scale-110 shrink-0',
                    isActive ? 'text-white' : 'text-slate-400 group-hover:text-slate-200'
                  )}
                />
                <span>{item.name}</span>
              </div>
              {isActive && (
                <span className="w-1.5 h-1.5 rounded-full bg-white shadow-xs shrink-0" />
              )}
            </Link>
          );
        })}

        {/* Public Showcase Link (Staff only) */}
        {!isTenant && (
          <div className="pt-4">
            <div className="px-3 pb-2 text-[10px] font-bold text-slate-500 uppercase tracking-wider">
              Public Portal
            </div>
            <Link
              href={`/p/${user?.organizationSlug || 'default-org'}`}
              target="_blank"
              onClick={() => onClose?.()}
              className="flex items-center justify-between px-3 py-2 rounded-xl text-xs sm:text-sm font-medium text-slate-400 hover:bg-slate-800/70 hover:text-slate-200 transition-all"
            >
              <div className="flex items-center gap-3">
                <ExternalLink className="w-4 h-4 text-slate-400" />
                <span>Public Showcase</span>
              </div>
              <span className="text-[10px] font-bold bg-slate-800 px-1.5 py-0.5 rounded text-slate-400">Live</span>
            </Link>
          </div>
        )}
      </nav>

      {/* User Footer */}
      <div className="p-4 border-t border-slate-800 bg-slate-950/60 shrink-0">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-slate-700 to-slate-800 flex items-center justify-center text-white font-bold text-xs border border-slate-700 shadow-xs shrink-0">
              {user?.fullName?.charAt(0) || 'U'}
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-white truncate leading-tight">
                {user?.fullName || 'Manager'}
              </p>
              <p className="text-[10px] text-slate-400 font-medium truncate">
                {user?.role || 'PROPERTY_MANAGER'}
              </p>
            </div>
          </div>
          <button
            onClick={logout}
            title="Logout"
            aria-label="Logout"
            className="p-2 text-slate-400 hover:text-rose-400 hover:bg-slate-800/80 rounded-xl transition-colors shrink-0"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop Persistent Sidebar */}
      <aside className="hidden md:flex w-64 flex-col min-h-screen border-r border-slate-800 shrink-0 sticky top-0 h-screen z-30">
        <NavContent />
      </aside>

      {/* Mobile Backdrop & Drawer */}
      {isOpen && (
        <div className="fixed inset-0 z-50 md:hidden animate-in fade-in duration-200">
          <div
            className="fixed inset-0 bg-slate-950/70 backdrop-blur-sm"
            onClick={onClose}
            aria-hidden="true"
          />
          <aside className="fixed inset-y-0 left-0 w-72 max-w-[85vw] shadow-2xl z-50 animate-in slide-in-from-left duration-200">
            <NavContent />
          </aside>
        </div>
      )}
    </>
  );
};
