/** biome-ignore-all lint/a11y/noLabelWithoutControl: <explanation> */
"use client";

import { Trip } from "@/lib/api-types";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { StatusBadge } from "@/components/shared/StatusBadge";
import {
  maskPan,
  formatDuration,
  formatCurrency,
  formatDateTime,
} from "@/lib/utils";
import { Clock, CreditCard, Bus, Bitcoin } from "lucide-react";
import { cn } from "@/lib/utils";

interface TripDetailsModalProps {
  trip: Trip | null;
  isOpen: boolean;
  onClose: () => void;
}

const getCardBrandIcon = (pan: string) => {
  const p = pan.replace(/\s/g, "");
  if (p.startsWith("4")) {
    return (
      <span className="text-[10px] font-black italic text-blue-600 tracking-tighter">
        VISA
      </span>
    );
  }
  if (p.startsWith("5")) {
    return (
      <div className="flex -space-x-1">
        <div className="w-3.5 h-3.5 rounded-full bg-[#ef4444]" />
        <div className="w-3.5 h-3.5 rounded-full bg-[#f59e0b]" />
      </div>
    );
  }
  if (p.startsWith("3")) {
    return (
      <span className="text-[10px] font-black text-emerald-600 tracking-widest leading-none">
        AMEX
      </span>
    );
  }
  if (p.startsWith("0x")) {
    return <Bitcoin className="w-4 h-4 text-purple-600" />;
  }
  return <CreditCard className="w-4 h-4 text-slate-400" />;
};

export function TripDetailsModal({
  trip,
  isOpen,
  onClose,
}: TripDetailsModalProps) {
  if (!trip) return null;

  console.log(trip);

  return (
    <Dialog open={isOpen} onOpenChange={(open: boolean) => !open && onClose()}>
      <DialogContent className="sm:max-w-[425px] bg-white border-slate-200 rounded-[32px] p-0 overflow-hidden shadow-2xl">
        <div className="bg-slate-50 p-6 border-b border-slate-100">
          <DialogHeader>
            <div className="flex items-center gap-3 mb-2">
              <div className="bg-teal-500/10 p-2.5 rounded-2xl">
                <Bus className="w-5 h-5 text-teal-600" />
              </div>
              <DialogTitle className="text-xl font-black uppercase tracking-tight text-slate-900">
                Trip Audit
              </DialogTitle>
            </div>
            <DialogDescription className="text-slate-400 text-[11px] font-bold uppercase tracking-wider">
              Reconstructed transit journey data & verification.
            </DialogDescription>
          </DialogHeader>
        </div>

        <div className="p-6 space-y-6">
          {/* Identity Section */}
          <div className="flex items-center justify-between bg-slate-50/50 p-4 rounded-2xl border border-slate-100">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 bg-white border border-slate-200 rounded-xl flex items-center justify-center shadow-sm">
                {getCardBrandIcon(trip.pan)}
              </div>
              <div className="flex flex-col">
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest mb-0.5">
                  Credential
                </span>
                <span className="theme-mono text-xs font-black text-slate-900 leading-none">
                  {maskPan(trip.pan)}
                </span>
              </div>
            </div>
            <StatusBadge status={trip.status} />
          </div>

          {/* Route Section */}
          <div className="space-y-4 px-1">
            <label className="text-[9px] font-black text-slate-400 uppercase tracking-[0.2em] block">
              Route Manifest
            </label>
            <div className="flex gap-4">
              <div className="flex flex-col items-center pt-1 pb-1">
                <div className="w-2.5 h-2.5 shrink-0 rounded-full bg-teal-500 ring-4 ring-teal-50" />
                <div className="w-[1px] flex-1 bg-slate-300 my-1" />
                <div
                  className={cn(
                    "w-2.5 h-2.5 shrink-0 rounded-full",
                    trip.endTime
                      ? "bg-teal-500 ring-4 ring-teal-50"
                      : "bg-white border-2 border-slate-200",
                  )}
                />
              </div>
              <div className="flex-1 flex flex-col justify-between gap-4">
                <div>
                  <h4 className="text-[13px] font-black text-slate-900 uppercase leading-none">
                    {trip.fromStop || "Origin Terminal"}
                  </h4>
                  <p className="text-[10px] text-slate-400 font-bold mt-1 flex items-center gap-1.5 uppercase">
                    <Clock className="w-3 h-3" />
                    {formatDateTime(trip.startTime)}
                  </p>
                </div>
                <div>
                  <h4
                    className={cn(
                      "text-[13px] font-black uppercase leading-none",
                      trip.toStop ? "text-slate-900" : "text-slate-400 italic",
                    )}
                  >
                    {trip.toStop || "Transit in Progress..."}
                  </h4>
                  {trip.endTime && (
                    <p className="text-[10px] text-teal-600 font-bold mt-1 flex items-center gap-1.5 uppercase">
                      <Clock className="w-3 h-3" />
                      {formatDateTime(trip.endTime)}
                    </p>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Device Context */}
          <div className="p-4 bg-slate-50/30 rounded-2xl border border-slate-100 grid grid-cols-2 gap-y-3 gap-x-6">
            <div className="flex flex-col">
              <span className="text-[8px] font-bold text-slate-400 uppercase tracking-widest mb-1">
                Device ID
              </span>
              <span className="text-[10px] font-mono font-bold text-slate-700">
                DV-{trip.deviceId?.slice(-6).toUpperCase()}
              </span>
            </div>
            <div className="flex flex-col text-right">
              <span className="text-[8px] font-bold text-slate-400 uppercase tracking-widest mb-1">
                Operator
              </span>
              <span className="text-[10px] font-bold text-slate-900 truncate">
                {trip.operator || "Unknown Network"}
              </span>
            </div>

            <div className="col-span-2 pt-3 border-t border-slate-100 flex justify-between items-center">
              <span className="text-[8px] font-bold text-slate-400 uppercase tracking-widest">
                Duration
              </span>
              <span className="text-[10px] font-bold text-slate-900 uppercase">
                {trip.status === "INCOMPLETE"
                  ? "Active Session"
                  : formatDuration(trip.duration)}
              </span>
            </div>
          </div>
        </div>

        {/* Footer with Charge */}
        <div className="bg-slate-900 p-8 flex items-center justify-between">
          <div className="flex flex-col">
            <span className="text-[9px] font-bold text-teal-400 uppercase tracking-[0.2em] mb-1">
              Final Settlement
            </span>
            <span className="text-3xl font-black text-white tabular-nums tracking-tighter">
              {formatCurrency(trip.chargeAmount)}
            </span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="px-8 py-3 bg-white text-slate-900 rounded-2xl text-[11px] font-black uppercase tracking-widest hover:bg-teal-50 transition-all active:scale-95 shadow-xl"
          >
            Acknowledge
          </button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
