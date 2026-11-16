'use client';

import React, { useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { Sidebar } from '@/components/layout/Sidebar';
import { Header } from '@/components/layout/Header';
import { useAuth } from '@/lib/auth/AuthContext';

import { ToastProvider } from '@/lib/toast/ToastContext';

export default function PortalLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isAuthenticated, isLoading, user } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = React.useState(false);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/login');
      return;
    }

    if (!isLoading && isAuthenticated && user) {
      if (user.role === 'TENANT') {
        const tenantAllowed = ['/app/tenant', '/app/invoices', '/app/receipts', '/app/tickets', '/app/meter-readings'];
        const isAllowed = tenantAllowed.some((route) => pathname === route || pathname.startsWith(route + '/'));
        if (!isAllowed) {
          router.replace('/app/tenant');
        }
      } else if (user.role === 'AGENT') {
        const agentForbidden = ['/app/settings', '/app/payouts'];
        const isForbidden = agentForbidden.some((route) => pathname === route || pathname.startsWith(route + '/'));
        if (isForbidden) {
          router.replace('/app');
        }
      }
    }
  }, [isAuthenticated, isLoading, user, pathname, router]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white text-sm">
        <div className="flex items-center gap-3">
          <svg className="animate-spin h-5 w-5 text-blue-500" viewBox="0 0 24 24" fill="none">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          <span className="font-semibold tracking-wide">Loading StayFile Workspace...</span>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) return null;

  return (
    <ToastProvider>
      <div className="min-h-screen flex bg-slate-50">
        <Sidebar
          isOpen={isMobileSidebarOpen}
          onClose={() => setIsMobileSidebarOpen(false)}
        />
        <div className="flex-1 flex flex-col min-w-0">
          <Header onMenuClick={() => setIsMobileSidebarOpen(true)} />
          <main className="flex-1 p-4 sm:p-6 md:p-8 max-w-7xl w-full mx-auto overflow-y-auto">
            {children}
          </main>
        </div>
      </div>
    </ToastProvider>
  );
}
