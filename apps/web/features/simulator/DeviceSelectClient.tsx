/** biome-ignore-all lint/suspicious/noNonNullAssertedOptionalChain: <explanation> */
"use client";

import { useEffect } from "react";
import { useTapForm } from "./TapFormContext";
import type { Devices } from "@/lib/api";

export function DeviceSelectClient({ devices = [] }: { devices: Devices[] }) {
  const { deviceId, setDeviceId } = useTapForm();

  useEffect(() => {
    if (!deviceId && devices.length > 0) {
      const d = devices[0] as Devices;
      setDeviceId(d.id);
    }
  }, [deviceId, devices, setDeviceId]);

  return (
    <select
      value={deviceId}
      onChange={(e) => setDeviceId(e.target.value)}
      className="font-mono text-[10px] text-slate-500 bg-white px-2 py-1 rounded border border-slate-200 outline-none hover:border-slate-300 focus:border-emerald-400 transition-all cursor-pointer truncate max-w-[140px]"
    >
      {devices.map((device) => (
        <option key={device.id} value={device.id}>
          {device.name}
        </option>
      ))}
    </select>
  );
}
