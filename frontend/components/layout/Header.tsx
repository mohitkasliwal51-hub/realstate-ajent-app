'use client';

import React from 'react';
import { Bell, Search, ShieldAlert } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';

export const Header: React.FC = () => {
  const { user } = useAuth();

  return (
    <header className="h-16 bg-white border-b border-slate-200 px-6 flex items-center justify-between sticky top-0 z-30 shadow-xs">
      {/* Search Input */}
      <div className="relative w-72">
        <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          placeholder="Search properties, units, tenants..."
          className="w-full pl-9 pr-4 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-all placeholder:text-slate-400"
        />
      </div>

      {/* Right Header Controls */}
      <div className="flex items-center gap-4">
        <div className="hidden sm:flex items-center gap-2 px-3 py-1 bg-blue-50 border border-blue-100 rounded-full text-xs font-medium text-blue-700">
          <ShieldAlert className="w-3.5 h-3.5" />
          <span>{user?.organizationName || 'Bhartiya SaaS Demo Org'}</span>
        </div>

        <button className="relative p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors">
          <Bell className="w-4 h-4" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-blue-600 rounded-full ring-2 ring-white" />
        </button>
      </div>
    </header>
  );
};
