import { Trip, TripFilters } from "@/lib/api-types";
import { Devices } from "@/lib/api";
import { KpiCards } from "./KpiCards";
import { MerchantClient } from "./MerchantClient";
import { formatCurrency } from "@/lib/utils";

interface MerchantPageProps {
  trips: Trip[];
  filters: TripFilters;
  devices: Devices[];
  error?: string;
}

export function computeStats(trips: Trip[]) {
  const totalRevenue = trips.reduce((acc, t) => acc + t.chargeAmount, 0);
  const activeSessions = trips.filter((t) => t.status === "INCOMPLETE").length;
  const settledJourneys = trips.filter((t) => t.status === "COMPLETED").length;
  return { totalRevenue, activeSessions, settledJourneys };
}

export function MerchantPage({
  trips,
  filters,
  devices,
  error,
}: MerchantPageProps) {
  const stats = computeStats(trips);

  return (
    <div className="flex flex-col gap-8">
      <header className="flex justify-between items-center mb-[-0.5rem]">
        <div>
          <h1 className="text-[28px] font-black m-0 tracking-tight text-slate-900 uppercase">
            Merchant Center
          </h1>
          <p className="text-slate-500 mt-1 text-[13px] font-medium border-l-[3px] border-teal-500 pl-3">
            Financial &amp; Analytics Central
          </p>
        </div>
        <div className="text-right">
          <div className="text-[10px] font-bold text-slate-500 uppercase tracking-[0.2em] mb-1 opacity-80">
            Session Earnings
          </div>
          <div className="text-[24px] font-black text-slate-900 tabular-nums leading-none">
            {formatCurrency(stats.totalRevenue)}
          </div>
        </div>
      </header>

      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-sm font-medium">
          {error}{" "}
          <a href="/merchant" className="underline">
            Retry
          </a>
        </div>
      )}

      <KpiCards stats={stats} />
      <MerchantClient
        trips={trips}
        initialFilters={filters}
        devices={devices}
      />
    </div>
  );
}
