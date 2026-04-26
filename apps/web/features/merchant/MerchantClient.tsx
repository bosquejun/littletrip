"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { TripFilters, Trip } from "@/lib/api-types";
import { Devices } from "@/lib/api";
import { TripTable } from "@/features/trips/TripTable";
import { Filters } from "@/features/trips/Filters";
import { Button } from "@/components/ui/button";
import { RefreshCcw } from "lucide-react";

interface MerchantClientProps {
  trips: Trip[];
  initialFilters: TripFilters;
  devices: Devices[];
}

export function MerchantClient({
  trips,
  initialFilters,
  devices,
}: MerchantClientProps) {
  const router = useRouter();
  const [filters, setFilters] = useState<TripFilters>(initialFilters);
  const [isPending, startTransition] = useTransition();

  const handleFilterChange = (next: TripFilters) => {
    setFilters(next);
    const params = new URLSearchParams();
    if (next.status) params.set("status", next.status);
    if (next.deviceId) params.set("deviceId", next.deviceId);
    if (next.datePreset && next.datePreset !== "all")
      params.set("datePreset", next.datePreset);
    startTransition(() => {
      router.push(`/merchant?${params.toString()}`);
    });
  };

  const handleRefresh = () => {
    startTransition(() => {
      router.refresh();
    });
  };

  return (
    <div className="flex flex-col gap-6 bg-white p-6 rounded-[32px] border border-slate-100 shadow-sm min-h-0 flex-1">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex items-center gap-4 flex-1">
          <Filters
            filters={filters}
            devices={devices}
            onFilterChange={handleFilterChange}
          />
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={handleRefresh}
          disabled={isPending}
          className="h-10 px-5 bg-slate-900 !text-white hover:bg-slate-900 hover:opacity-90 border-none flex items-center gap-2 font-bold text-xs rounded-full shadow-lg  transition-all active:scale-95"
        >
          <RefreshCcw
            className={`w-3.5 h-3.5 ${isPending ? "animate-spin" : ""}`}
          />
          {isPending ? "Syncing..." : "Refresh"}
        </Button>
      </header>
      <div className="flex-1 min-h-[400px]">
        <TripTable trips={trips} isLoading={isPending} devices={devices} />
      </div>
    </div>
  );
}
