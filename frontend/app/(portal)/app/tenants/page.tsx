'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  Users,
  Plus,
  Mail,
  Phone,
  Building2,
  BedDouble,
  Copy,
  Check,
  Share2,
  ExternalLink,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { tenantApi, Tenant, VerificationStatus } from '@/lib/api/tenantApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { TableSkeleton } from '@/components/ui/TableSkeleton';
import { SearchFilterBar } from '@/components/ui/SearchFilterBar';
import { EmptyState } from '@/components/ui/EmptyState';
import { useToast } from '@/lib/toast/ToastContext';

interface TenantMetadata {
  inviteToken?: string;
  propertyId?: string;
  propertyName?: string;
  unitId?: string;
  unitNumber?: string;
  sharingType?: string;
  monthlyRent?: number;
  securityDeposit?: number;
  tokenPaid?: number;
  expectedMoveInDate?: string;
  notes?: string;
  status?: string;
  invitedAt?: string;
}

const parseMetadata = (raw?: string): TenantMetadata | null => {
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
};

export default function TenantsPage() {
  const router = useRouter();
  const { user } = useAuth();
  const { toast } = useToast();
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [kycFilter, setKycFilter] = useState('ALL');
  const [copiedTenantId, setCopiedTenantId] = useState<string | null>(null);

  const loadTenants = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      const data = await tenantApi.getTenants();
      setTenants(data || []);
    } catch (err) {
      console.error('Failed to load tenants', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadTenants();
  }, [user?.organizationId]);

  const getInviteLink = (tenant: Tenant) => {
    const meta = parseMetadata(tenant.metadata);
    const origin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:3000';
    const params = new URLSearchParams({
      invite: meta?.inviteToken || tenant.id,
      propertyId: meta?.propertyId || '',
      propertyName: meta?.propertyName || '',
      unitId: meta?.unitId || '',
      unitNumber: meta?.unitNumber || '',
      sharingType: meta?.sharingType || '',
      monthlyRent: String(meta?.monthlyRent || 0),
      securityDeposit: String(meta?.securityDeposit || 0),
      tokenPaid: String(meta?.tokenPaid || 0),
      fullName: tenant.fullName,
      phone: tenant.phone,
      email: tenant.email || '',
    });
    return `${origin}/kyc/tenant?${params.toString()}`;
  };

  const handleCopyInvite = (tenant: Tenant) => {
    const link = getInviteLink(tenant);
    navigator.clipboard.writeText(link);
    setCopiedTenantId(tenant.id);
    toast.success('Self-KYC Invite Link copied!');
    setTimeout(() => setCopiedTenantId(null), 2500);
  };

  const handleWhatsAppShare = (tenant: Tenant) => {
    const meta = parseMetadata(tenant.metadata);
    const link = getInviteLink(tenant);
    const cleanPhone = tenant.phone.replace(/[^0-9]/g, '');
    const targetPhone = cleanPhone.startsWith('91') ? cleanPhone : `91${cleanPhone}`;
    const message = `Hello ${tenant.fullName}, your tenancy onboarding at ${
      meta?.propertyName || 'StayFile Property'
    } is ready. Please complete your KYC verification and review your tenancy agreement here: ${link}`;
    window.open(`https://wa.me/${targetPhone}?text=${encodeURIComponent(message)}`, '_blank');
  };

  const getKycBadge = (tenant: Tenant) => {
    const meta = parseMetadata(tenant.metadata);
    if (tenant.kycStatus === 'VERIFIED') {
      return <Badge variant="success" dot>KYC Verified</Badge>;
    }
    if (tenant.kycStatus === 'REJECTED') {
      return <Badge variant="danger" dot>KYC Rejected</Badge>;
    }
    if (meta?.status === 'INVITED') {
      return <Badge variant="warning" dot>Invited (Pending KYC)</Badge>;
    }
    return <Badge variant="warning" dot>KYC Pending</Badge>;
  };

  const filteredTenants = tenants.filter((t) => {
    const meta = parseMetadata(t.metadata);
    const matchesSearch =
      searchTerm === '' ||
      t.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      t.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      t.phone?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      meta?.propertyName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      meta?.unitNumber?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesKyc = kycFilter === 'ALL' || t.kycStatus === kycFilter;
    return matchesSearch && matchesKyc;
  });

  const filterOptions = [
    { id: 'ALL', label: 'All', count: tenants.length },
    { id: 'VERIFIED', label: 'Verified', count: tenants.filter((t) => t.kycStatus === 'VERIFIED').length },
    { id: 'PENDING', label: 'Pending', count: tenants.filter((t) => t.kycStatus === 'PENDING').length },
    { id: 'REJECTED', label: 'Rejected', count: tenants.filter((t) => t.kycStatus === 'REJECTED').length },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Tenants & Onboarding</h1>
          <p className="text-xs text-slate-500 mt-1">
            Invite prospective tenants, manage property/bed allocations, and track self-service KYC status.
          </p>
        </div>
        <Link href="/app/tenants/new">
          <Button>
            <Plus className="w-4 h-4 mr-2" /> Onboard Tenant
          </Button>
        </Link>
      </div>

      <SearchFilterBar
        searchTerm={searchTerm}
        onSearchChange={setSearchTerm}
        placeholder="Search tenants by name, email, phone, or property..."
        filterOptions={filterOptions}
        activeFilter={kycFilter}
        onFilterChange={setKycFilter}
      />

      {isLoading ? (
        <TableSkeleton rows={5} columns={5} />
      ) : filteredTenants.length === 0 ? (
        <EmptyState
          icon={Users}
          title={tenants.length === 0 ? 'No tenants registered yet' : 'No tenants match your filter'}
          description={
            tenants.length === 0
              ? 'Onboard your first tenant to dispatch digital KYC and lease agreement invitations.'
              : 'Try adjusting your search query or selecting a different KYC status filter.'
          }
          actionLabel="Onboard Tenant"
          onAction={() => router.push('/app/tenants/new')}
        />
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <caption className="sr-only">Tenant onboarding records and KYC status</caption>
              <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-semibold">
                <tr>
                  <th className="px-6 py-3.5">Tenant Name</th>
                  <th className="px-6 py-3.5">Contact Info</th>
                  <th className="px-6 py-3.5">Property & Room / Bed</th>
                  <th className="px-6 py-3.5">KYC Status</th>
                  <th className="px-6 py-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {filteredTenants.map((tenant) => {
                  const meta = parseMetadata(tenant.metadata);
                  const isCopied = copiedTenantId === tenant.id;
                  const isPending = tenant.kycStatus !== 'VERIFIED';

                  return (
                    <tr key={tenant.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-purple-100 text-purple-700 font-bold flex items-center justify-center text-xs">
                            {tenant.fullName.charAt(0)}
                          </div>
                          <div>
                            <p className="font-bold text-slate-900 text-sm">{tenant.fullName}</p>
                            {tenant.emergencyContactName ? (
                              <p className="text-[11px] text-slate-400">
                                Emergency: {tenant.emergencyContactName} ({tenant.emergencyContactPhone})
                              </p>
                            ) : (
                              <p className="text-[11px] text-slate-400">
                                {meta?.expectedMoveInDate ? `Move-in: ${meta.expectedMoveInDate}` : 'New Prospective Tenant'}
                              </p>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 space-y-1">
                        <div className="flex items-center gap-1.5 text-slate-600">
                          <Mail className="w-3.5 h-3.5 text-slate-400" /> {tenant.email || '—'}
                        </div>
                        <div className="flex items-center gap-1.5 text-slate-600">
                          <Phone className="w-3.5 h-3.5 text-slate-400" /> {tenant.phone}
                        </div>
                      </td>
                      <td className="px-6 py-4 space-y-1">
                        {meta?.propertyName ? (
                          <>
                            <div className="flex items-center gap-1.5 font-semibold text-slate-800">
                              <Building2 className="w-3.5 h-3.5 text-slate-400" />
                              {meta.propertyName}
                            </div>
                            {meta.unitNumber && (
                              <div className="flex items-center gap-1.5 text-slate-500 text-[11px]">
                                <BedDouble className="w-3.5 h-3.5 text-slate-400" />
                                {meta.unitNumber} {meta.sharingType ? `(${meta.sharingType.replace('_', ' ')})` : ''}
                              </div>
                            )}
                          </>
                        ) : tenant.permanentAddress && tenant.permanentAddress !== 'Pending Self-KYC' ? (
                          <p className="text-slate-500 truncate max-w-xs">{tenant.permanentAddress}</p>
                        ) : (
                          <span className="text-slate-400 italic">Allocation Pending</span>
                        )}
                      </td>
                      <td className="px-6 py-4">{getKycBadge(tenant)}</td>
                      <td className="px-6 py-4 text-right">
                        {isPending ? (
                          <div className="flex items-center justify-end gap-1.5">
                            <button
                              type="button"
                              onClick={() => handleCopyInvite(tenant)}
                              title="Copy Self-KYC Invite Link"
                              className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs font-semibold rounded-lg border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 transition-colors shadow-2xs"
                            >
                              {isCopied ? (
                                <>
                                  <Check className="w-3.5 h-3.5 text-emerald-600" />
                                  <span className="text-emerald-600 font-medium">Copied</span>
                                </>
                              ) : (
                                <>
                                  <Copy className="w-3.5 h-3.5 text-slate-500" />
                                  <span>Copy Invite</span>
                                </>
                              )}
                            </button>
                            <button
                              type="button"
                              onClick={() => handleWhatsAppShare(tenant)}
                              title="Share Invite on WhatsApp"
                              className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs font-semibold rounded-lg border border-emerald-200 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 transition-colors shadow-2xs"
                            >
                              <Share2 className="w-3.5 h-3.5 text-emerald-600" />
                              <span>WhatsApp</span>
                            </button>
                            <a
                              href={getInviteLink(tenant)}
                              target="_blank"
                              rel="noreferrer"
                              title="Open Self-KYC Page"
                              className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            >
                              <ExternalLink className="w-4 h-4" />
                            </a>
                          </div>
                        ) : (
                          <Badge variant="success">Completed</Badge>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
