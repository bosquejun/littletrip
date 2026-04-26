/** biome-ignore-all lint/suspicious/noArrayIndexKey: <explanation> */
"use client";

import { useState, useEffect } from "react";
import { Trip } from "@/lib/api-types";
import { Devices } from "@/lib/api";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { TripDetailsModal } from "./TripDetailsModal";
import { maskPan, formatDuration, formatCurrency } from "@/lib/utils";
import {
  Bus,
  MapPin,
  Clock,
  ArrowRight,
  Bitcoin,
  CreditCard,
} from "lucide-react";
import { cn } from "@/lib/utils";

interface TripTableProps {
  trips: Trip[];
  isLoading: boolean;
  devices: Devices[];
}

const getCardBrandIcon = (pan: string) => {
  const p = pan.replace(/\s/g, "");
  if (p.startsWith("4")) {
    return (
      <div className="flex items-center gap-1">
        <span className="text-[10px] font-black italic text-blue-600 tracking-tighter">
          VISA
        </span>
      </div>
    );
  }
  if (p.startsWith("5")) {
    return (
      <div className="flex -space-x-1">
        <div className="w-3 h-3 rounded-full bg-[#ef4444]" />
        <div className="w-3 h-3 rounded-full bg-[#f59e0b]" />
      </div>
    );
  }
  if (p.startsWith("3")) {
    return (
      <div className="bg-emerald-600 px-1 rounded-[2px]">
        <span className="text-[8px] font-black text-white tracking-widest leading-none">
          AMEX
        </span>
      </div>
    );
  }
  if (p.startsWith("0x")) {
    return <Bitcoin className="w-3.5 h-3.5 text-purple-600" />;
  }
  return <CreditCard className="w-3.5 h-3.5 text-slate-400" />;
};

export function TripTable({ trips, isLoading, devices }: TripTableProps) {
  const [selectedTrip, setSelectedTrip] = useState<Trip | null>(null);
  const [currentPage, setCurrentPage] = useState(1);

  const itemsPerPage = 8;
  const totalPages = Math.ceil(trips.length / itemsPerPage);

  const getDeviceName = (deviceId?: string) => {
    if (!deviceId) return "---";
    const device = devices.find((d) => d.id === deviceId);
    return device?.name ?? `DV-${deviceId.slice(-6).toUpperCase()}`;
  };

  // Reset page when trips array changes
  // biome-ignore lint/correctness/useExhaustiveDependencies: <explanation>
  useEffect(() => {
    const timer = setTimeout(() => setCurrentPage(1), 0);
    return () => clearTimeout(timer);
  }, [trips]);

  const currentTrips = trips.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage,
  );

  if (isLoading) {
    return (
      <div className="w-full space-y-4">
        {[...Array(5)].map((_, i) => (
          <div
            key={i}
            className="h-14 w-full animate-pulse bg-slate-100 rounded-lg"
          />
        ))}
      </div>
    );
  }

  if (trips.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20 bg-white rounded-2xl border border-dashed border-slate-200">
        <div className="bg-slate-50 p-4 rounded-full mb-4">
          <Bus className="w-8 h-8 text-slate-400" />
        </div>
        <h3 className="text-xl font-semibold mb-2 text-slate-900">
          No trips found
        </h3>
        <p className="text-slate-500 text-center max-w-[300px] text-sm">
          Try adjusting your filters or simulate a new tap event to see
          reconstructed trips.
        </p>
      </div>
    );
  }

  return (
    <>
      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden flex-1 flex flex-col">
        <div className="overflow-auto flex-1">
          <Table>
            <TableHeader className="bg-slate-50/80 border-b border-slate-200">
              <TableRow className="hover:bg-transparent border-none">
                <TableHead className="py-4 h-11 text-[11px] font-bold text-slate-400 uppercase tracking-widest pl-6">
                  Wallet
                </TableHead>
                <TableHead className="h-11 text-[11px] font-bold text-slate-400 uppercase tracking-widest">
                  Route Path
                </TableHead>
                <TableHead className="h-11 text-[11px] font-bold text-slate-400 uppercase tracking-widest">
                  Operator
                </TableHead>
                <TableHead className="h-11 text-[11px] font-bold text-slate-400 uppercase tracking-widest">
                  Timeline
                </TableHead>
                <TableHead className="h-11 text-[11px] font-bold text-slate-400 uppercase tracking-widest">
                  Billing
                </TableHead>
                <TableHead className="h-11 text-[11px] font-bold text-slate-400 uppercase tracking-widest text-center pr-6">
                  Status
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {currentTrips.map((trip) => (
                <TableRow
                  key={trip.id}
                  className="group border-slate-100 hover:bg-slate-50/50 transition-colors cursor-pointer"
                  onClick={() => setSelectedTrip(trip)}
                >
                  <TableCell className="pl-6">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg bg-slate-50 border border-slate-100 flex items-center justify-center shrink-0">
                        {getCardBrandIcon(trip.pan)}
                      </div>
                      <div className="flex flex-col">
                        <span className="font-mono text-[13px] font-bold text-slate-900 tracking-wider inline-flex items-center">
                          {maskPan(trip.pan)}
                        </span>
                        <span className="text-[10px] text-slate-400 font-bold uppercase tracking-tight">
                          Active ID: {trip.id.split("-")[1]?.substring(0, 8)}
                        </span>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2 py-0.5">
                      <div className="flex items-center bg-slate-50 border border-slate-100 rounded-full px-3 py-1.5 gap-2 group-hover:border-teal-200 group-hover:bg-teal-50/30 transition-colors">
                        <div className="flex items-center gap-2">
                          <span
                            className={cn(
                              "text-[12px] font-bold transition-colors",
                              trip.fromStop
                                ? "text-slate-900"
                                : "text-slate-400",
                            )}
                          >
                            {trip.fromStop || "Origin"}
                          </span>
                          <ArrowRight className="w-3 h-3 text-slate-500" />
                          <span
                            className={cn(
                              "text-[12px] font-bold transition-colors",
                              trip.toStop ? "text-slate-900" : "text-slate-400",
                            )}
                          >
                            {trip.toStop || "---"}
                          </span>
                        </div>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="text-[12px] font-bold text-slate-900 tracking-tight">
                        {trip.operator || "Unknown Network"}
                      </span>
                      <span className="text-[10px] text-slate-400 font-bold uppercase tracking-widest font-mono mt-0.5">
                        {getDeviceName(trip.deviceId)}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col gap-1">
                      <div className="flex items-center gap-1.5 text-slate-900 font-bold text-[13px] tabular-nums" suppressHydrationWarning>
                        <Clock className="w-3 h-3 text-slate-400" />
                        <span suppressHydrationWarning>{new Date(trip.startTime).toLocaleTimeString([], {
                          hour: "2-digit",
                          minute: "2-digit",
                          hour12: false,
                        })}</span>
                        {trip.endTime && (
                          <>
                            <span className="text-slate-300 mx-0.5">—</span>
                            <span suppressHydrationWarning>{new Date(trip.endTime).toLocaleTimeString([], {
                              hour: "2-digit",
                              minute: "2-digit",
                              hour12: false,
                            })}</span>
                          </>
                        )}
                      </div>
                      <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider flex items-center gap-1">
                        {trip.endTime ? (
                          <span className="text-teal-600 bg-teal-50 px-1 rounded">
                            {formatDuration(trip.duration)} TOTAL
                          </span>
                        ) : (
                          <span className="text-amber-600 bg-amber-50 px-1 rounded animate-pulse text-[9px]">
                            En Route
                          </span>
                        )}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="theme-mono text-[14px] font-black text-slate-900 tracking-tighter">
                        {formatCurrency(trip.chargeAmount)}
                      </span>
                      <span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest">
                        Base Fare
                      </span>
                    </div>
                  </TableCell>
                  <TableCell className="text-center pr-6">
                    <StatusBadge status={trip.status} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
        <div className="mt-auto p-4 px-6 border-t border-slate-100 flex justify-between items-center bg-slate-50/50">
          <div className="text-[11px] font-bold text-slate-400 tracking-widest uppercase">
            Showing {(currentPage - 1) * itemsPerPage + 1} -{" "}
            {Math.min(currentPage * itemsPerPage, trips.length)} of{" "}
            {trips.length} entries
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="px-4 py-1.5 text-[10px] font-bold uppercase tracking-widest bg-white border border-slate-200 rounded-lg text-slate-500 hover:bg-slate-50 hover:border-slate-300 disabled:opacity-50 disabled:cursor-not-allowed transition-all active:scale-95 shadow-sm"
            >
              Prev
            </button>
            <div className="text-[11px] font-bold text-slate-600 px-2 tabular-nums">
              {currentPage} / {Math.max(1, totalPages)}
            </div>
            <button
              type="button"
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages || totalPages === 0}
              className="px-4 py-1.5 text-[10px] font-bold uppercase tracking-widest bg-white border border-slate-200 rounded-lg text-slate-500 hover:bg-slate-50 hover:border-slate-300 disabled:opacity-50 disabled:cursor-not-allowed transition-all active:scale-95 shadow-sm"
            >
              Next
            </button>
          </div>
        </div>
      </div>

      <TripDetailsModal
        trip={selectedTrip}
        isOpen={!!selectedTrip}
        onClose={() => setSelectedTrip(null)}
      />
    </>
  );
}
