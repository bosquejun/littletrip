"use client";

import { createContext, useContext } from "react";
import type { TransitStop } from "@/lib/api-types";

interface TapFormState {
  pan: string;
  deviceId: string;
  stopId: string;
  stops: TransitStop[];
  setPan: (pan: string) => void;
  setDeviceId: (id: string) => void;
  setStopId: (id: string) => void;
  setStops: (stops: TransitStop[]) => void;
}

export const TapFormContext = createContext<TapFormState | null>(null);

export function useTapForm() {
  const ctx = useContext(TapFormContext);
  if (!ctx) throw new Error("useTapForm must be used within TapSimulatorInteractive");
  return ctx;
}
