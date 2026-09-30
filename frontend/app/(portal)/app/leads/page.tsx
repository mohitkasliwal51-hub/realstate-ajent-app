'use client';

import React, { useEffect, useState } from 'react';
import { UserCheck, Phone, Mail, Globe, ArrowRight } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { leadApi, PropertyLead, LeadStatus } from '@/lib/api/leadApi';
import { Badge } from '@/components/ui/Badge';

export default function LeadsPage() {
  const { user } = useAuth();
  const [leads, setLeads] = useState<PropertyLead[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const loadLeads = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      const data = await leadApi.getLeadsByOrganization(user.organizationId);
      setLeads(data || []);
    } catch (err) {
      console.error('Failed to load leads', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadLeads();
  }, [user?.organizationId]);

  const handleStatusChange = async (leadId: string, status: LeadStatus) => {
    if (!user?.organizationId) return;
    try {
      await leadApi.updateLeadStatus(leadId, user.organizationId, status);
      await loadLeads();
    } catch (err) {
      alert('Failed to update lead status');
    }
  };

  const getStatusBadge = (status: LeadStatus) => {
    switch (status) {
      case 'NEW': return <Badge variant="info">New Inquiry</Badge>;
      case 'CONTACTED': return <Badge variant="warning">Contacted</Badge>;
      case 'VISITED': return <Badge variant="purple">Site Visited</Badge>;
      case 'CONVERTED': return <Badge variant="success">Converted</Badge>;
      case 'LOST': return <Badge variant="danger">Lost</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">CRM Lead Pipeline</h1>
        <p className="text-xs text-slate-500 mt-1">
          Track inquiries from public showcase, WhatsApp bot, 99acres, and NoBroker.
        </p>
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-slate-400 text-sm">Loading leads...</div>
      ) : leads.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-slate-300 space-y-3">
          <UserCheck className="w-12 h-12 text-slate-400 mx-auto" />
          <h3 className="font-bold text-slate-900">No leads in pipeline</h3>
          <p className="text-xs text-slate-500">Inquiries captured from showcase portals or WhatsApp will appear here.</p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-semibold">
              <tr>
                <th className="px-6 py-3.5">Lead Name</th>
                <th className="px-6 py-3.5">Contact Details</th>
                <th className="px-6 py-3.5">Source</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5 text-right">Update Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-medium">
              {leads.map((lead) => (
                <tr key={lead.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="px-6 py-4">
                    <p className="font-bold text-slate-900 text-sm">{lead.name}</p>
                    {lead.propertyName && <p className="text-[11px] text-slate-500">Property: {lead.propertyName}</p>}
                  </td>
                  <td className="px-6 py-4 space-y-1">
                    <div className="flex items-center gap-1.5 text-slate-600">
                      <Phone className="w-3.5 h-3.5 text-slate-400" /> {lead.phone}
                    </div>
                    {lead.email && (
                      <div className="flex items-center gap-1.5 text-slate-600">
                        <Mail className="w-3.5 h-3.5 text-slate-400" /> {lead.email}
                      </div>
                    )}
                  </td>
                  <td className="px-6 py-4">
                    <Badge variant="purple">{lead.source}</Badge>
                  </td>
                  <td className="px-6 py-4">{getStatusBadge(lead.status)}</td>
                  <td className="px-6 py-4 text-right">
                    <select
                      value={lead.status}
                      onChange={(e) => handleStatusChange(lead.id, e.target.value as LeadStatus)}
                      className="px-2 py-1 bg-slate-100 border border-slate-200 rounded text-xs focus:outline-none"
                    >
                      <option value="NEW">NEW</option>
                      <option value="CONTACTED">CONTACTED</option>
                      <option value="VISITED">VISITED</option>
                      <option value="CONVERTED">CONVERTED</option>
                      <option value="LOST">LOST</option>
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
