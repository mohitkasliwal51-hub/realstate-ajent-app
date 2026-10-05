'use client';

import React, { useState } from 'react';
import {
  Zap,
  ShieldCheck,
  ChevronDown,
  ChevronUp,
  Image as ImageIcon,
  Users,
  Calculator,
  ExternalLink,
} from 'lucide-react';
import { UtilitySplitMetadata } from '@/lib/api/invoiceApi';
import { formatCurrency } from '@/lib/utils';
import { resolveFileUrl } from '@/lib/api/fileApi';
import { Modal } from '@/components/ui/Modal';

interface UtilitySplitAuditCardProps {
  metadataString?: string;
  defaultOpen?: boolean;
}

export const UtilitySplitAuditCard: React.FC<UtilitySplitAuditCardProps> = ({
  metadataString,
  defaultOpen = false,
}) => {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  const [previewPhoto, setPreviewPhoto] = useState<{ url: string; title: string } | null>(null);

  if (!metadataString) return null;

  let metadata: UtilitySplitMetadata | null = null;
  try {
    metadata = JSON.parse(metadataString);
  } catch {
    return null;
  }

  if (!metadata || metadata.utilityType !== 'ELECTRICITY') return null;

  return (
    <div className="border border-blue-100 bg-blue-50/50 rounded-2xl p-4 my-2 transition-all">
      <div
        className="flex items-center justify-between cursor-pointer select-none"
        onClick={() => setIsOpen(!isOpen)}
      >
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-blue-600 text-white flex items-center justify-center shadow-xs">
            <Zap className="w-4 h-4" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-xs text-slate-900">
                Transparent Multi-Tier Electricity Audit
              </span>
              <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-700 bg-emerald-100/80 px-2 py-0.5 rounded-full">
                <ShieldCheck className="w-3 h-3" /> Audit Verified
              </span>
            </div>
            <p className="text-[11px] text-slate-500">
              Discom Master Units ({metadata.masterDiscomUnits}) − Sub-Meters ({metadata.totalAcUnits}) = Common Split ({metadata.commonAreaUnits} units)
            </p>
          </div>
        </div>

        <button
          type="button"
          className="text-xs font-semibold text-blue-700 hover:text-blue-800 flex items-center gap-1 bg-white px-2.5 py-1 rounded-lg border border-blue-200 shadow-2xs"
        >
          {isOpen ? 'Hide Formula' : 'View Formula'}
          {isOpen ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </button>
      </div>

      {isOpen && (
        <div className="mt-4 pt-4 border-t border-blue-100 space-y-4 text-xs text-slate-700">
          {/* Formula summary banner */}
          <div className="bg-white p-3 rounded-xl border border-blue-200/80 shadow-2xs flex items-start gap-2.5">
            <Calculator className="w-4 h-4 text-blue-600 mt-0.5 shrink-0" />
            <div>
              <p className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">Split Engine Formula</p>
              <p className="text-slate-600 text-[11px] mt-0.5">
                <span className="font-semibold text-slate-900">Common Share</span> = (Master Discom Units − Total AC Units) ÷ Total PG Residents ({metadata.totalPropertyResidents})
                <br />
                <span className="font-semibold text-slate-900">Room AC Share</span> = Room AC Sub-Meter Units ÷ Room Occupants ({metadata.roomOccupantCount})
              </p>
            </div>
          </div>

          {/* Breakdown cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {/* Common Area Split */}
            <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-2xs space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-bold text-slate-800 flex items-center gap-1.5">
                  <Users className="w-3.5 h-3.5 text-blue-600" /> 1. Common Area Share
                </span>
                <span className="font-bold text-blue-700">
                  {formatCurrency(metadata.commonShareAmount)}
                </span>
              </div>
              <div className="text-[11px] text-slate-600 space-y-1">
                <div className="flex justify-between">
                  <span>Common Units Total:</span>
                  <span className="font-medium text-slate-900">{metadata.commonAreaUnits} kWh</span>
                </div>
                <div className="flex justify-between">
                  <span>Total PG Residents:</span>
                  <span className="font-medium text-slate-900">{metadata.totalPropertyResidents} persons</span>
                </div>
                <div className="flex justify-between">
                  <span>Your Share of Common Units:</span>
                  <span className="font-semibold text-blue-600">{metadata.tenantCommonShareUnits} kWh</span>
                </div>
                <div className="flex justify-between border-t border-slate-100 pt-1 text-slate-500">
                  <span>Rate:</span>
                  <span>₹{metadata.ratePerUnit} / unit</span>
                </div>
              </div>
            </div>

            {/* Room AC Split */}
            <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-2xs space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-bold text-slate-800 flex items-center gap-1.5">
                  <Zap className="w-3.5 h-3.5 text-amber-500" /> 2. Room AC Share
                </span>
                <span className="font-bold text-amber-700">
                  {formatCurrency(metadata.roomAcAmount)}
                </span>
              </div>
              <div className="text-[11px] text-slate-600 space-y-1">
                <div className="flex justify-between">
                  <span>Room AC Meter Total:</span>
                  <span className="font-medium text-slate-900">{metadata.roomAcTotalUnits} kWh</span>
                </div>
                <div className="flex justify-between">
                  <span>Your Room Occupants:</span>
                  <span className="font-medium text-slate-900">{metadata.roomOccupantCount} roommates</span>
                </div>
                <div className="flex justify-between">
                  <span>Your Share of Room AC:</span>
                  <span className="font-semibold text-amber-600">{metadata.tenantRoomAcUnits} kWh</span>
                </div>
                <div className="flex justify-between border-t border-slate-100 pt-1 text-slate-500">
                  <span>Rate:</span>
                  <span>₹{metadata.ratePerUnit} / unit</span>
                </div>
              </div>
            </div>
          </div>

          {/* Submeter Photo Proof Section */}
          {(metadata.submeterPhotoUrl || metadata.masterMeterPhotoUrl) && (
            <div className="bg-white p-3 rounded-xl border border-slate-200 flex flex-wrap items-center gap-4">
              <span className="text-[11px] font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
                <ImageIcon className="w-3.5 h-3.5 text-slate-500" /> Meter Photo Proofs:
              </span>

              {metadata.submeterPhotoUrl && (
                <button
                  type="button"
                  onClick={() =>
                    setPreviewPhoto({
                      url: resolveFileUrl(metadata!.submeterPhotoUrl!),
                      title: 'Room AC Sub-Meter Reading Proof',
                    })
                  }
                  className="flex items-center gap-1.5 text-xs text-blue-700 font-semibold hover:underline bg-blue-50 px-2.5 py-1 rounded-lg border border-blue-200"
                >
                  <ExternalLink className="w-3 h-3" /> View Room AC Meter Photo
                </button>
              )}

              {metadata.masterMeterPhotoUrl && (
                <button
                  type="button"
                  onClick={() =>
                    setPreviewPhoto({
                      url: resolveFileUrl(metadata!.masterMeterPhotoUrl!),
                      title: 'Master Discom Meter Bill Proof',
                    })
                  }
                  className="flex items-center gap-1.5 text-xs text-blue-700 font-semibold hover:underline bg-slate-50 px-2.5 py-1 rounded-lg border border-slate-200"
                >
                  <ExternalLink className="w-3 h-3" /> View Master Discom Photo
                </button>
              )}
            </div>
          )}
        </div>
      )}

      {/* Photo Preview Modal */}
      {previewPhoto && (
        <Modal
          isOpen={true}
          onClose={() => setPreviewPhoto(null)}
          title={previewPhoto.title}
          description="High-resolution meter reading capture logged during the billing cycle audit."
        >
          <div className="space-y-4">
            <div className="bg-slate-900 rounded-xl overflow-hidden flex items-center justify-center max-h-[450px]">
              <img
                src={previewPhoto.url}
                alt={previewPhoto.title}
                className="max-h-[440px] max-w-full object-contain"
              />
            </div>
            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => setPreviewPhoto(null)}
                className="px-4 py-2 text-xs font-semibold bg-slate-100 hover:bg-slate-200 rounded-lg text-slate-700"
              >
                Close Preview
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
