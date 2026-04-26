import { Suspense } from "react";
import { TapSimulatorInteractive } from "./TapSimulatorInteractive";
import { DeviceSelectServer } from "./DeviceSelectServer";
import { StopsServer } from "./StopsServer";

function DeviceSelectSkeleton() {
  return (
    <div className="h-7 w-[140px] rounded border border-slate-200 bg-slate-100 animate-pulse" />
  );
}

function StopsSkeleton() {
  return (
    <div className="px-2">
      <div className="relative border-l-2 border-slate-200 ml-3 space-y-3 py-2">
        {Array.from({ length: 4 }).map((_, i) => (
          // biome-ignore lint/suspicious/noArrayIndexKey: skeleton
          <div key={i} className="relative pl-6">
            <div className="absolute -left-[9px] top-1/2 -translate-y-1/2 w-4 h-4 rounded-full border-2 border-slate-200 bg-white" />
            <div className="p-2 space-y-1">
              <div className="h-4 w-28 bg-slate-100 rounded animate-pulse" />
              <div className="h-3 w-16 bg-slate-100 rounded animate-pulse" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function TapSimulator() {
  return (
    <TapSimulatorInteractive
      deviceSelect={
        <Suspense fallback={<DeviceSelectSkeleton />}>
          <DeviceSelectServer />
        </Suspense>
      }
      stops={
        <Suspense fallback={<StopsSkeleton />}>
          <StopsServer />
        </Suspense>
      }
    />
  );
}
