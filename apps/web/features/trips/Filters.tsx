"use client";

import { TripFilters, TripStatus } from "@/lib/api-types";
import { Devices } from "@/lib/api";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { X, Bus } from "lucide-react";
import { cn } from "@/lib/utils";

interface FiltersProps {
  filters: TripFilters;
  devices: Devices[];
  onFilterChange: (filters: TripFilters) => void;
}

const STATUS_CONFIG: Record<TripStatus, { dot: string; label: string }> = {
  COMPLETED: { dot: "bg-teal-500", label: "Completed" },
  INCOMPLETE: { dot: "bg-red-500", label: "Incomplete" },
  CANCELLED: { dot: "bg-slate-400", label: "Cancelled" },
  IN_PROGRESS: { dot: "bg-amber-500", label: "In Progress" },
};

const STATUS_LABELS: Record<TripStatus, string> = {
  COMPLETED: "Completed",
  INCOMPLETE: "Incomplete",
  CANCELLED: "Cancelled",
  IN_PROGRESS: "In Progress",
};

const DATE_PRESETS = [
  { value: "all", label: "All" },
  { value: "today", label: "Today" },
  { value: "week", label: "This Week" },
] as const;

export function Filters({ filters, devices, onFilterChange }: FiltersProps) {
  const activePreset = filters.datePreset ?? "all";

  const activeChips: { key: keyof TripFilters; label: string }[] = [];
  if (filters.status) {
    activeChips.push({
      key: "status",
      label: `Status: ${STATUS_LABELS[filters.status]}`,
    });
  }
  if (filters.deviceId) {
    const device = devices.find((d) => d.id === filters.deviceId);
    activeChips.push({
      key: "deviceId",
      label: `Device: ${device?.name ?? filters.deviceId}`,
    });
  }
  if (filters.datePreset && filters.datePreset !== "all") {
    const preset = DATE_PRESETS.find((p) => p.value === filters.datePreset);
    activeChips.push({ key: "datePreset", label: `Date: ${preset?.label}` });
  }

  const clearFilter = (key: keyof TripFilters) =>
    onFilterChange({ ...filters, [key]: undefined });

  const clearAll = () => onFilterChange({});

  return (
    <div className="flex flex-col gap-2 flex-1">
      <div className="flex flex-wrap items-center gap-3">
        {/* Status */}
        <Select
          value={filters.status ?? "ALL"}
          onValueChange={(value) =>
            onFilterChange({
              ...filters,
              status: value === "ALL" ? undefined : (value as TripStatus),
            })
          }
        >
          <SelectTrigger className="h-9 w-[150px] bg-white border-[#e2e8f0] text-xs shadow-none">
            <SelectValue placeholder="All Statuses" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Statuses</SelectItem>
            <SelectItem value="COMPLETED">Completed</SelectItem>
            <SelectItem value="INCOMPLETE">Incomplete</SelectItem>
            <SelectItem value="CANCELLED">Cancelled</SelectItem>
            <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
          </SelectContent>
        </Select>

        {/* Device */}
        <Select
          value={devices.find((d) => d.id === filters.deviceId)?.name ?? "ALL"}
          onValueChange={(value) =>
            onFilterChange({
              ...filters,
              deviceId: value === "ALL" ? null : value,
            })
          }
        >
          <SelectTrigger className="h-9 w-[180px] bg-white border-[#e2e8f0] text-xs shadow-none">
            <SelectValue placeholder="All Devices" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Devices</SelectItem>
            {devices.map((device) => (
              <SelectItem key={device.id} value={device.id}>
                {device.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        {/* Date preset toggle */}
        <div className="flex items-center gap-1 bg-slate-100 rounded-full p-1">
          {DATE_PRESETS.map((preset) => (
            <button
              key={preset.value}
              type="button"
              onClick={() =>
                onFilterChange({
                  ...filters,
                  datePreset: preset.value === "all" ? undefined : preset.value,
                })
              }
              className={cn(
                "px-3 py-1 rounded-full text-xs font-bold transition-all",
                activePreset === preset.value
                  ? "bg-white text-slate-900 shadow-sm"
                  : "text-slate-500 hover:text-slate-700",
              )}
            >
              {preset.label}
            </button>
          ))}
        </div>
      </div>

      {/* Active filter chips */}
      {activeChips.length > 0 && (
        <div className="flex flex-wrap items-center gap-2">
          {activeChips.map(({ key, label }) => (
            <span
              key={key}
              className="inline-flex items-center gap-1 bg-teal-50 border border-teal-200 text-teal-700 rounded-full px-2.5 py-0.5 text-[11px] font-bold"
            >
              {label}
              <button
                type="button"
                onClick={() => clearFilter(key)}
                className="hover:text-teal-900 transition-colors"
                aria-label={`Remove ${label} filter`}
              >
                <X className="w-3 h-3" />
              </button>
            </span>
          ))}
          <button
            type="button"
            onClick={clearAll}
            className="text-[11px] font-bold text-slate-400 hover:text-slate-600 transition-colors underline"
          >
            Clear all
          </button>
        </div>
      )}
    </div>
  );
}
