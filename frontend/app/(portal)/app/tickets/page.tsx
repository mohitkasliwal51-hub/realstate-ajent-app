'use client';

import React, { useEffect, useState } from 'react';
import { Wrench, Plus, CheckCircle2, Clock, AlertTriangle } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import { ticketApi, MaintenanceTicket } from '@/lib/api/ticketApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';

export default function TicketsPage() {
  const { user } = useAuth();
  const [tickets, setTickets] = useState<MaintenanceTicket[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const loadTickets = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      const data = await ticketApi.getTicketsByOrganization();
      setTickets(data || []);
    } catch (err) {
      console.error('Failed to load tickets', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadTickets();
  }, [user?.organizationId]);

  const handleStatusChange = async (ticketId: string, status: string) => {
    if (!user?.organizationId) return;
    try {
      await ticketApi.updateTicketStatus(ticketId, status as any);
      await loadTickets();
    } catch (err) {
      alert('Failed to update ticket status');
    }
  };

  const getPriorityBadge = (priority: string) => {
    switch (priority) {
      case 'URGENT': return <Badge variant="danger">Urgent</Badge>;
      case 'HIGH': return <Badge variant="warning">High</Badge>;
      case 'MEDIUM': return <Badge variant="info">Medium</Badge>;
      default: return <Badge variant="default">Low</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Maintenance Desk</h1>
        <p className="text-xs text-slate-500 mt-1">
          Track plumbing, electrical, Wi-Fi, and appliance repair requests logged by tenants.
        </p>
      </div>

      {isLoading ? (
        <div className="text-center py-12 text-slate-400 text-sm">Loading tickets...</div>
      ) : tickets.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-slate-300 space-y-3">
          <Wrench className="w-12 h-12 text-slate-400 mx-auto" />
          <h3 className="font-bold text-slate-900">No active tickets</h3>
          <p className="text-xs text-slate-500">Maintenance requests logged by residents will appear here.</p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-semibold">
              <tr>
                <th className="px-6 py-3.5">Ticket & Category</th>
                <th className="px-6 py-3.5">Tenant & Unit</th>
                <th className="px-6 py-3.5">Priority</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-medium">
              {tickets.map((ticket) => (
                <tr key={ticket.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="px-6 py-4">
                    <p className="font-bold text-slate-900 text-sm">{ticket.title}</p>
                    <p className="text-[11px] text-slate-400">{ticket.category} • {ticket.description}</p>
                  </td>
                  <td className="px-6 py-4">
                    <p className="font-bold text-slate-800">{ticket.tenantName}</p>
                    <p className="text-[11px] text-slate-500">Unit {ticket.unitNumber}</p>
                  </td>
                  <td className="px-6 py-4">{getPriorityBadge(ticket.priority)}</td>
                  <td className="px-6 py-4">
                    <Badge variant={ticket.status === 'RESOLVED' || ticket.status === 'CLOSED' ? 'success' : 'warning'}>
                      {ticket.status}
                    </Badge>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <select
                      value={ticket.status}
                      onChange={(e) => handleStatusChange(ticket.id, e.target.value)}
                      className="px-2 py-1 bg-slate-100 border border-slate-200 rounded text-xs focus:outline-none"
                    >
                      <option value="OPEN">OPEN</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="RESOLVED">RESOLVED</option>
                      <option value="CLOSED">CLOSED</option>
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
