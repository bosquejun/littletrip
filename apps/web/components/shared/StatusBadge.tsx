import { TripStatus } from "@/lib/api-types";
import { cn } from "@/lib/utils";

interface StatusBadgeProps {
  status: TripStatus;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = {
    COMPLETED: {
      color: "bg-teal-50 text-teal-700 border-teal-200",
      dot: "bg-teal-500",
      label: "Settled",
    },
    INCOMPLETE: {
      color: "bg-red-50 text-red-700 border-red-200",
      dot: "bg-red-500 animate-pulse",
      label: "In Complete",
    },
    CANCELLED: {
      color: "bg-slate-50 text-slate-600 border-slate-200",
      dot: "bg-slate-400",
      label: "Voided",
    },
    IN_PROGRESS: {
      color: "bg-amber-50 text-amber-700 border-amber-200",
      dot: "bg-amber-500 animate-pulse",
      label: "Active",
    },
  };

  const { color, dot, label } = config[status] || {};

  return (
    <div
      className={cn(
        "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md shadow-sm border",
        color,
      )}
    >
      <div className="relative flex h-2 w-2 items-center justify-center">
        {status === "INCOMPLETE" && (
          <span
            className={cn(
              "absolute inline-flex h-full w-full rounded-full opacity-75",
              dot,
            )}
          />
        )}
        <span
          className={cn(
            "relative inline-flex rounded-full h-1.5 w-1.5",
            dot.replace("animate-pulse", ""),
          )}
        />
      </div>
      <span className="text-[10px] font-bold uppercase tracking-widest leading-none mt-[1px]">
        {label}
      </span>
    </div>
  );
}
