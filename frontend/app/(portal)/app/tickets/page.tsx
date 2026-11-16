'use client';

import React, { useEffect, useState } from 'react';
import { Wrench, Plus } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';
import {
  ticketApi,
  MaintenanceTicket,
  TicketCategory,
  TicketPriority,
  TICKET_CATEGORIES,
  TICKET_PRIORITIES,
  TicketCreatePayload,
} from '@/lib/api/ticketApi';
import { tenantApi, Tenant } from '@/lib/api/tenantApi';
import { leaseApi, Lease } from '@/lib/api/leaseApi';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { TableSkeleton } from '@/components/ui/TableSkeleton';
import { SearchFilterBar } from '@/components/ui/SearchFilterBar';
import { EmptyState } from '@/components/ui/EmptyState';
import { getErrorMessage } from '@/lib/utils';
import { useToast } from '@/lib/toast/ToastContext';

export default function TicketsPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const isTenant = user?.role === 'TENANT';

  const [tickets, setTickets] = useState<MaintenanceTicket[]>([]);
  const [tenantProfile, setTenantProfile] = useState<Tenant | null>(null);
  const [tenantLease, setTenantLease] = useState<Lease | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmittingTicket, setIsSubmittingTicket] = useState(false);
  const [ticketForm, setTicketForm] = useState<TicketCreatePayload>({
    unitId: '',
    tenantId: '',
    title: '',
    description: '',
    category: 'PLUMBING',
    priority: 'MEDIUM',
  });

  const loadTickets = async () => {
    if (!user?.organizationId) return;
    setIsLoading(true);
    try {
      if (isTenant) {
        const [profileRes, leaseRes] = await Promise.all([
          tenantApi.getMyProfile().catch(() => null),
          leaseApi.getMyLease().catch(() => null),
        ]);
        setTenantProfile(profileRes);
        setTenantLease(leaseRes);

        if (profileRes?.id) {
          const tenantTickets = await ticketApi.getTicketsByTenant(profileRes.id);
          setTickets(tenantTickets || []);
        } else {
          setTickets([]);
        }
      } else {
        const data = await ticketApi.getTicketsByOrganization();
        setTickets(data || []);
      }
    } catch (err) {
      console.error('Failed to load tickets', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadTickets();
  }, [user?.organizationId, user?.role]);

  const handleStatusChange = async (ticketId: string, status: string) => {
    if (!user?.organizationId || isTenant) return;
    try {
      await ticketApi.updateTicketStatus(ticketId, status as any);
      toast.success(`Ticket status updated to ${status}`);
      await loadTickets();
    } catch (err) {
      toast.error('Failed to update ticket status');
    }
  };

  const handleOpenModal = () => {
    setTicketForm({
      unitId: tenantLease?.unitId || '',
      tenantId: tenantProfile?.id || '',
      title: '',
      description: '',
      category: 'PLUMBING',
      priority: 'MEDIUM',
    });
    setIsModalOpen(true);
  };

  const handleCreateTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!ticketForm.unitId || !ticketForm.tenantId) {
      toast.warning('Cannot create ticket: active lease or unit assignment not found.');
      return;
    }
    setIsSubmittingTicket(true);
    try {
      await ticketApi.createTicket(ticketForm);
      toast.success('Maintenance ticket submitted successfully!');
      setIsModalOpen(false);
      await loadTickets();
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to log maintenance ticket'));
    } finally {
      setIsSubmittingTicket(false);
    }
  };

  const getPriorityBadge = (priority: string) => {
    switch (priority) {
      case 'URGENT': return <Badge variant="danger" dot pulse>Urgent</Badge>;
      case 'HIGH': return <Badge variant="warning" dot>High</Badge>;
      case 'MEDIUM': return <Badge variant="info">Medium</Badge>;
      default: return <Badge variant="default">Low</Badge>;
    }
  };

  const filteredTickets = tickets.filter((t) => {
    const matchesSearch =
      searchTerm === '' ||
      t.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      t.category.toLowerCase().includes(searchTerm.toLowerCase()) ||
      t.tenantName?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || t.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const filterOptions = [
    { id: 'ALL', label: 'All', count: tickets.length },
    { id: 'OPEN', label: 'Open', count: tickets.filter((t) => t.status === 'OPEN').length },
    { id: 'IN_PROGRESS', label: 'In Progress', count: tickets.filter((t) => t.status === 'IN_PROGRESS').length },
    { id: 'RESOLVED', label: 'Resolved', count: tickets.filter((t) => t.status === 'RESOLVED').length },
    { id: 'CLOSED', label: 'Closed', count: tickets.filter((t) => t.status === 'CLOSED').length },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            {isTenant ? 'My Maintenance Tickets' : 'Maintenance Desk'}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            {isTenant
              ? 'Raise and track repair requests for plumbing, electrical, Wi-Fi, and appliances.'
              : 'Track plumbing, electrical, Wi-Fi, and appliance repair requests logged by tenants.'}
          </p>
        </div>
        {isTenant && tenantLease && (
          <Button onClick={handleOpenModal}>
            <Plus className="w-4 h-4 mr-2" /> Raise Ticket
          </Button>
        )}
      </div>

      <SearchFilterBar
        searchTerm={searchTerm}
        onSearchChange={setSearchTerm}
        placeholder="Search tickets by title, category, or tenant..."
        filterOptions={filterOptions}
        activeFilter={statusFilter}
        onFilterChange={setStatusFilter}
      />

      {isLoading ? (
        <TableSkeleton rows={5} columns={5} />
      ) : filteredTickets.length === 0 ? (
        <EmptyState
          icon={Wrench}
          title={isTenant ? 'No maintenance tickets raised' : 'No tickets match your filter'}
          description={
            isTenant
              ? 'Have an issue in your unit? Raise a ticket and the property manager will be notified.'
              : tickets.length === 0
              ? 'Maintenance requests logged by residents will appear here.'
              : 'Try clearing your search query or selecting a different status tab.'
          }
          actionLabel={isTenant && tenantLease ? 'Raise Ticket' : undefined}
          onAction={isTenant && tenantLease ? handleOpenModal : undefined}
        />
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 uppercase tracking-wider text-slate-500 font-semibold">
              <tr>
                <th className="px-6 py-3.5">Ticket & Category</th>
                <th className="px-6 py-3.5">{isTenant ? 'Unit' : 'Tenant & Unit'}</th>
                <th className="px-6 py-3.5">Priority</th>
                <th className="px-6 py-3.5">Status</th>
                {!isTenant && <th className="px-6 py-3.5 text-right">Action</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-medium">
              {filteredTickets.map((ticket) => (
                <tr key={ticket.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="px-6 py-4">
                    <p className="font-bold text-slate-900 text-sm">{ticket.title}</p>
                    <p className="text-[11px] text-slate-400">{ticket.category} • {ticket.description}</p>
                  </td>
                  <td className="px-6 py-4">
                    {!isTenant && <p className="font-bold text-slate-800">{ticket.tenantName}</p>}
                    <p className="text-[11px] text-slate-500 font-semibold">Unit {ticket.unitNumber}</p>
                  </td>
                  <td className="px-6 py-4">{getPriorityBadge(ticket.priority)}</td>
                  <td className="px-6 py-4">
                    <Badge variant={ticket.status === 'RESOLVED' || ticket.status === 'CLOSED' ? 'success' : 'warning'}>
                      {ticket.status}
                    </Badge>
                  </td>
                  {!isTenant && (
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
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Tenant Ticket Raise Modal */}
      {isTenant && (
        <Modal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          title="Raise Maintenance Ticket"
          description="Report a repair or service request to your property manager"
        >
          <form onSubmit={handleCreateTicket} className="space-y-4">
            <Input
              label="Issue Summary *"
              placeholder="e.g. Kitchen tap leaking"
              value={ticketForm.title}
              onChange={(e) => setTicketForm({ ...ticketForm, title: e.target.value })}
              required
            />

            <div className="grid grid-cols-2 gap-3">
              <div className="w-full flex flex-col gap-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">Category</label>
                <select
                  className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={ticketForm.category}
                  onChange={(e) => setTicketForm({ ...ticketForm, category: e.target.value as TicketCategory })}
                >
                  {TICKET_CATEGORIES.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>

              <div className="w-full flex flex-col gap-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">Priority</label>
                <select
                  className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={ticketForm.priority}
                  onChange={(e) => setTicketForm({ ...ticketForm, priority: e.target.value as TicketPriority })}
                >
                  {TICKET_PRIORITIES.map((p) => (
                    <option key={p} value={p}>{p}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="w-full flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">Details / Description</label>
              <textarea
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[90px]"
                placeholder="Describe the issue and best time for technician visit..."
                value={ticketForm.description}
                onChange={(e) => setTicketForm({ ...ticketForm, description: e.target.value })}
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <Button type="button" variant="outline" onClick={() => setIsModalOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" isLoading={isSubmittingTicket}>
                Submit Request
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
