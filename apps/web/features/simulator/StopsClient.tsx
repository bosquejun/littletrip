"use client";
/** biome-ignore-all lint/a11y/noStaticElementInteractions: simulator */
/** biome-ignore-all lint/a11y/useKeyWithClickEvents: simulator */

import { useEffect } from "react";
import { cn } from "@/lib/utils";
import { useTapForm } from "./TapFormContext";
import type { TransitStop } from "@/lib/api-types";

export function StopsClient({ stops }: { stops: TransitStop[] }) {
  const { stopId, setStopId, setStops } = useTapForm();

  useEffect(() => {
    setStops(stops);
    if (stops.length > 0 && !stopId) {
      setStopId(stops[0]!.id);
    }
  }, [stops, stopId, setStopId, setStops]);

  return (
    <div className="px-2">
      <div className="relative border-l-2 border-slate-200 ml-3 space-y-3 py-2">
        {stops.map((stop) => {
          const isActive = stopId === stop.id;
          return (
            <div
              key={stop.id}
              className="relative pl-6 cursor-pointer group"
              onClick={() => setStopId(stop.id)}
            >
              <div
                className={cn(
                  "absolute -left-[9px] top-1/2 -translate-y-1/2 w-4 h-4 rounded-full border-2 bg-white transition-all flex items-center justify-center",
                  isActive
                    ? "border-teal-500 scale-110"
                    : "border-slate-300 group-hover:border-teal-400",
                )}
              >
                {isActive && (
                  <div className="w-1.5 h-1.5 bg-teal-500 rounded-full" />
                )}
              </div>
              <div
                className={cn(
                  "flex items-center justify-between p-2 rounded-lg transition-all border",
                  isActive
                    ? "bg-teal-50 border-teal-200"
                    : "bg-transparent border-transparent hover:bg-slate-50",
                )}
              >
                <div className="flex flex-col">
                  <span
                    className={cn(
                      "text-sm font-bold",
                      isActive ? "text-teal-900" : "text-slate-600",
                    )}
                  >
                    {stop.name}
                  </span>
                  <span className="text-[10px] text-slate-400 font-mono tracking-wide">
                    {stop.id.slice(0, 8)}…
                  </span>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
