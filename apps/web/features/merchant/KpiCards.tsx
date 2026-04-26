import { DollarSign, TrendingUp, Users, ArrowUpRight } from "lucide-react";
import { cn } from "@/lib/utils";

interface KpiStats {
  totalRevenue: number;
  activeSessions: number;
  settledJourneys: number;
}

interface KpiCardsProps {
  stats: KpiStats;
}

const CARDS = [
  {
    label: "Total Revenue",
    key: "totalRevenue" as const,
    icon: DollarSign,
    trend: "+12.5%",
    color: "text-emerald-600",
    isCurrency: true,
  },
  {
    label: "Active Sessions",
    key: "activeSessions" as const,
    icon: Users,
    trend: "+3",
    color: "text-teal-600",
  },
  {
    label: "Settled Journeys",
    key: "settledJourneys" as const,
    icon: TrendingUp,
    trend: "98.2%",
    color: "text-emerald-500",
  },
];

export function KpiCards({ stats }: KpiCardsProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
      {CARDS.map((card) => (
        <div
          key={card.label}
          className="bg-white p-6 rounded-3xl border border-slate-100 shadow-sm hover:shadow-md transition-shadow relative overflow-hidden group"
        >
          <div className="flex justify-between items-start relative z-10">
            <div className="space-y-1">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">
                {card.label}
              </span>
              <div className="text-2xl font-black tabular-nums tracking-tight">
                {card.isCurrency
                  ? `$${stats[card.key].toFixed(2)}`
                  : stats[card.key]}
              </div>
            </div>
            <div
              className={cn(
                "p-2 rounded-xl bg-slate-50 group-hover:bg-slate-100 transition-colors",
                card.color,
              )}
            >
              <card.icon className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-center gap-1.5 relative z-10">
            <span className="text-[11px] font-bold text-emerald-600 flex items-center">
              <ArrowUpRight className="w-3 h-3" />
              {card.trend}
            </span>
            <span className="text-[11px] text-slate-400 font-medium">
              vs last month
            </span>
          </div>
          <div className="absolute top-[-20%] right-[-10%] w-24 h-24 rounded-full bg-slate-50/50 blur-2xl -z-0" />
        </div>
      ))}
    </div>
  );
}
