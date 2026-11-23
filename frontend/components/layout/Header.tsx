'use client';

import React from 'react';
import { Bell, Search, Building2, Home, Menu } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';

export interface HeaderProps {
  onMenuClick?: () => void;
}

export const Header: React.FC<HeaderProps> = ({ onMenuClick }) => {
  const { user } = useAuth();
  const isTenant = user?.role === 'TENANT';

  return (
    <header className="h-16 bg-white/80 backdrop-blur-md border-b border-slate-200/80 px-4 sm:px-6 flex items-center justify-between sticky top-0 z-20 shadow-xs">
      {/* Left: Mobile Menu Toggle & Search Bar */}
      <div className="flex items-center gap-3 flex-1 max-w-md">
        {onMenuClick && (
          <button
            onClick={onMenuClick}
            aria-label="Open navigation menu"
            className="md:hidden p-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-xl transition-colors shrink-0"
          >
            <Menu className="w-5 h-5" />
          </button>
        )}

        <form role="search" className="relative w-full" onSubmit={(event) => event.preventDefault()}>
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            name="search"
            type="text"
            placeholder={isTenant ? 'Search documents, bills & tickets...' : 'Search properties, units, tenants...'}
            aria-label={isTenant ? 'Search documents and requests' : 'Search properties and tenants'}
            className="w-full pl-9 pr-4 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-all placeholder:text-slate-400 text-slate-900"
          />
        </form>
      </div>

      {/* Right Header Controls */}
      <div className="flex items-center gap-3">
        <div className="hidden sm:flex items-center gap-2 px-3 py-1 bg-blue-50 border border-blue-100/80 rounded-full text-xs font-semibold text-blue-700 shadow-xs">
          {isTenant ? <Home className="w-3.5 h-3.5 text-blue-600" /> : <Building2 className="w-3.5 h-3.5 text-blue-600" />}
          <span className="truncate max-w-[160px]">{user?.organizationName || 'StayFile Workspace'}</span>
        </div>

        <button
          aria-label="Notifications"
          className="relative p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors"
        >
          <Bell className="w-4 h-4" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-blue-600 rounded-full ring-2 ring-white" />
        </button>
      </div>
    </header>
  );
};
