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
  ShieldCheck,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { cn } from '@/lib/utils';

const navItems = [
  { name: 'Dashboard', href: '/app', icon: LayoutDashboard },
  { name: 'Properties', href: '/app/properties', icon: Building2 },
  { name: 'Tenants', href: '/app/tenants', icon: Users },
  { name: 'Leases', href: '/app/leases', icon: FileText },
  { name: 'Receipts', href: '/app/receipts', icon: Receipt },
  { name: 'Settings', href: '/app/settings', icon: Settings },
];

export const Sidebar: React.FC = () => {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  return (
    <aside className="w-64 bg-slate-900 text-slate-300 flex flex-col min-h-screen border-r border-slate-800">
      {/* Brand Header */}
      <div className="h-16 flex items-center gap-3 px-6 border-b border-slate-800">
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

      {/* Navigation */}
      <nav className="flex-1 px-4 py-6 space-y-1.5 overflow-y-auto">
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
                  'w-5 h-5 transition-transform group-hover:scale-105',
                  isActive ? 'text-white' : 'text-slate-400 group-hover:text-slate-200'
                )}
              />
              {item.name}
            </Link>
          );
        })}

        {/* Public Showcase Link */}
        <div className="pt-6 px-3 pb-2 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
          Public Portal
        </div>
        <Link
          href="/p/default-org"
          target="_blank"
          className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-slate-400 hover:bg-slate-800/60 hover:text-slate-200 transition-all"
        >
          <ExternalLink className="w-5 h-5 text-slate-400" />
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
